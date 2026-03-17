const express = require("express");
const cors = require("cors");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const admin = require("firebase-admin");
const crypto = require("crypto");
const nodemailer = require("nodemailer");
const bcrypt = require("bcrypt");
const axios = require("axios");
const AIClassifier = require("./classifier");

const SALT_ROUNDS = 10;

// ===============================
// 📧 NODEMAILER SETUP
// ===============================
// Set SMTP_USER and SMTP_PASS environment variables for real email sending.
// Falls back gracefully to console-log-only mode if not configured.
const mailTransporter = (process.env.SMTP_USER && process.env.SMTP_PASS)
  ? nodemailer.createTransport({
      service: "gmail",
      auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS }
    })
  : null;

async function sendResetEmail(to, resetUrl) {
  const subject = "Reset Your SCMS Password";
  const html = `
    <div style="font-family:Inter,sans-serif;max-width:480px;margin:auto;padding:32px;background:#0F1B2D;color:#E2E8F0;border-radius:12px">
      <h2 style="color:#3B82F6">🔐 Password Reset</h2>
      <p>You requested a password reset for your SCMS account.</p>
      <p>Click the button below to reset your password. This link expires in <strong>1 hour</strong>.</p>
      <a href="${resetUrl}" style="display:inline-block;margin:20px 0;padding:12px 28px;background:#3B82F6;color:#fff;border-radius:8px;text-decoration:none;font-weight:600">Reset Password</a>
      <p style="color:#64748B;font-size:12px">If you did not request this, please ignore this email.</p>
    </div>
  `;

  if (mailTransporter) {
    await mailTransporter.sendMail({ from: `"SCMS" <${process.env.SMTP_USER}>`, to, subject, html });
    console.log(`📧 Password reset email sent to ${to}`);
  } else {
    // Dev fallback — log the link so you can test without SMTP
    console.log(`\n=================================\n📧 MAIL (DEV MODE — no SMTP configured)\n=================================\nTO: ${to}\nSUBJECT: ${subject}\nRESET LINK: ${resetUrl}\n=================================\n(Set SMTP_USER and SMTP_PASS env vars to enable real email sending)\n=================================\n`);
  }
}

const app = express();

// ===============================
// 🔥 FIREBASE INIT
// ===============================
let db = null;
let bucket = null;
try {
  const serviceAccount = require("./serviceAccountKey.json");
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    storageBucket: "scms-17eef.firebasestorage.app" // 🪣 Firebase Storage bucket
  });
  db = admin.firestore();
  bucket = admin.storage().bucket();
  console.log("✅ Firebase Admin SDK Initialised & Connected to LIVE Firestore + Storage");
} catch (e) {
  console.log("⚠️ No serviceAccountKey.json found. Enabling LOCAL FIREBASE EMULATOR.");
  console.log("   (Start it in another terminal: npx firebase-tools emulators:start --project demo-scms-local)");

  // Magic: point Firebase SDK to local emulator
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";

  admin.initializeApp({ projectId: "demo-scms-local" });
  db = admin.firestore();
  console.log("✅ Connected to Firebase Local Emulator on 127.0.0.1:8080.");
}

app.use(cors());
app.use(express.json());
app.use("/uploads", express.static(path.join(__dirname, "uploads")));
app.use(express.static(path.join(__dirname, "public")));

// ===============================
// 📁 Multer Setup (Memory Storage → Firebase Storage)
// ===============================
// We use memoryStorage so the file buffer is available for Firebase Storage upload.
// Photos are no longer saved to local disk.
const upload = multer({ storage: multer.memoryStorage() });

// -------------------------------------------------------
// 🪣 Helper: Upload buffer to Firebase Storage, return public URL
// -------------------------------------------------------
async function uploadToFirebaseStorage(fileBuffer, originalName, mimeType) {
  if (!bucket) {
    // Emulator mode: no real Storage available, return null
    return null;
  }
  const ext = path.extname(originalName) || ".jpg";
  const fileName = `complaints/${Date.now()}${ext}`;
  const file = bucket.file(fileName);

  await file.save(fileBuffer, {
    metadata: { contentType: mimeType || "image/jpeg" }
  });

  // Make the file publicly readable
  await file.makePublic();

  // Return the permanent public download URL
  const publicUrl = `https://storage.googleapis.com/${bucket.name}/${fileName}`;
  console.log(`✅ Photo uploaded to Firebase Storage: ${publicUrl}`);
  return publicUrl;
}

// ===============================
// 🔢 Auto-sequencer Helper for MySQL-like Integer IDs
// ===============================
async function getNextId(collectionName) {
  if (!db) return Date.now(); // Fallback if no Firebase connected
  const counterRef = db.collection('counters').doc(collectionName);

  try {
    const res = await db.runTransaction(async (transaction) => {
      const doc = await transaction.get(counterRef);
      const newId = doc.exists ? doc.data().seq + 1 : 1;
      transaction.set(counterRef, { seq: newId });
      return newId;
    });
    return res;
  } catch (err) {
    console.error(`ID Generation Error for ${collectionName}:`, err);
    return Date.now(); // emergency fallback
  }
}

// ================================================
// 🔐 AUTH (Signup/Login into Firestore users collection)
// ================================================

app.post("/signup", async (req, res) => {
  const { name, phone, email, password } = req.body;
  if (!name || !phone || !password)
    return res.status(400).json({ success: false, message: "Name, phone, and password required" });

  if (!/^\d{10}$/.test(phone)) {
    return res.status(400).json({ success: false, message: "Phone number must be exactly 10 digits" });
  }

  try {
    // Check dupe phone
    const snapshot = await db.collection("users").where("phone", "==", phone).get();
    if (!snapshot.empty) {
      return res.status(409).json({ success: false, message: "Phone already registered" });
    }

    const newId = await getNextId("users");

    // 🔐 Hash password with bcrypt before storing
    const hashedPassword = await bcrypt.hash(password, SALT_ROUNDS);

    await db.collection("users").doc(newId.toString()).set({
      id: newId,
      name,
      phone,
      email: email ? email.toLowerCase() : null,
      password: hashedPassword, // ✅ Stored as bcrypt hash
      points: 0,
      badgeLevel: "Citizen",
      fcm_token: null,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    res.json({ success: true, userId: newId });
  } catch (err) {
    console.error("❌ Signup error:", err);
    res.status(500).json({ success: false });
  }
});

app.post("/login", async (req, res) => {
  const { phone, password } = req.body;
  if (!phone || !password)
    return res.status(400).json({ success: false, message: "Phone and password required" });

  if (!/^\d{10}$/.test(phone)) {
    return res.status(400).json({ success: false, message: "Phone number must be exactly 10 digits" });
  }

  try {
    // Fetch user by phone, then compare password with bcrypt
    const snapshot = await db.collection("users")
      .where("phone", "==", phone)
      .limit(1)
      .get();

    if (snapshot.empty) {
      return res.status(401).json({ success: false, message: "Invalid credentials" });
    }

    const userData = snapshot.docs[0].data();

    // 🔐 bcrypt.compare handles both hashed and plain-text legacy passwords gracefully
    const passwordMatch = await bcrypt.compare(password, userData.password).catch(() => false);
    // Legacy fallback: also allow plain-text match (for existing users before hashing was added)
    const legacyMatch = !passwordMatch && (userData.password === password);

    if (!passwordMatch && !legacyMatch) {
      return res.status(401).json({ success: false, message: "Invalid credentials" });
    }

    // If legacy user logged in with plain text, transparently upgrade their hash
    if (legacyMatch) {
      const upgraded = await bcrypt.hash(password, SALT_ROUNDS);
      await db.collection("users").doc(snapshot.docs[0].id).update({ password: upgraded });
      console.log(`🔄 Upgraded plain-text password to bcrypt hash for user ${userData.id}`);
    }

    res.json({
      success: true,
      user: {
        id: userData.id,
        name: userData.name,
        phone: userData.phone,
        points: userData.points || 0,
        badgeLevel: userData.badgeLevel || "Citizen"
      }
    });
  } catch (err) {
    console.error("❌ Login error:", err);
    res.status(500).json({ success: false });
  }
});

app.post("/forgot-password", async (req, res) => {
  const { email } = req.body;

  if (!email) return res.status(400).json({ success: false, message: "Email required" });
  if (!email.toLowerCase().endsWith("@gmail.com")) return res.status(400).json({ success: false, message: "Only Gmail addresses are supported for reset" });

  try {
    let query = db.collection("users").where("email", "==", email.toLowerCase()).limit(1);
    const snapshot = await query.get();

    if (snapshot.empty) return res.status(404).json({ success: false, message: "Email not found. If you don't have an email set, please contact support." });

    const userDoc = snapshot.docs[0];

    // Generate a secure mock reset token
    const token = crypto.randomBytes(20).toString("hex");

    // Save to Firestore with timestamp logic
    await db.collection("users").doc(userDoc.id).update({
      resetPasswordToken: token,
      resetPasswordExpires: Date.now() + 3600000 // 1 hour
    });

    // Generate the reset link
    const rootUrl = `http://localhost:3000/reset-password.html?token=${token}`;

    // 📧 Send real email (or log if SMTP not configured)
    await sendResetEmail(email, rootUrl);

    res.json({ success: true, message: "Password reset instructions sent to your email!", resetUrl: rootUrl });
  } catch (err) {
    console.error("❌ Forgot password error:", err);
    res.status(500).json({ success: false });
  }
});

// Mocking the Password Update action from the email Token Link form representation
app.post("/reset-password", async (req, res) => {
  const { token, newPassword } = req.body;
  if (!token || !newPassword) return res.status(400).json({ success: false, message: "Token and password required." });

  try {
    const snapshot = await db.collection("users")
      .where("resetPasswordToken", "==", token)
      .where("resetPasswordExpires", ">", Date.now())
      .limit(1)
      .get();

    if (snapshot.empty) return res.status(400).json({ success: false, message: "Password reset token is invalid or has expired." });

    const userDoc = snapshot.docs[0];

    // 🔐 Hash the new password before saving
    const hashedNew = await bcrypt.hash(newPassword, SALT_ROUNDS);
    await db.collection("users").doc(userDoc.id).update({
      password: hashedNew,
      resetPasswordToken: null,
      resetPasswordExpires: null
    });

    res.json({ success: true, message: "Password successfully updated!" });
  } catch (error) {
    res.status(500).json({ success: false });
  }
});

// ================================================
// 📝 SUBMIT COMPLAINT (+10 pts to reporter)
// ================================================
app.post("/complaint", upload.single("photo"), async (req, res) => {
  const { category, address, latitude, longitude, description, user_id } = req.body;

  const place = (address && address.trim() !== "") ? address.trim() : "Address not available";
  const uid = user_id ? parseInt(user_id) : null;
  const lat = latitude !== undefined ? latitude : null;
  const lon = longitude !== undefined ? longitude : null;

  try {
    // 🪣 Upload photo to Firebase Storage (if provided)
    let photoUrl = null;
    if (req.file) {
      photoUrl = await uploadToFirebaseStorage(
        req.file.buffer,
        req.file.originalname,
        req.file.mimetype
      );
    }

    // 🧠 AI PROCESSOR: Predict Severity + Category if applicable
    const prediction = await AIClassifier.classifyComplaint(description || category || "Unknown Hazard");
    let finalCategory = category || prediction.suggestedCategory;
    let finalSeverity = prediction.severity;

    const newId = await getNextId("complaints");

    await db.collection("complaints").doc(newId.toString()).set({
      id: newId,
      user_id: uid,
      category: finalCategory,
      description,
      place,
      latitude: lat,
      longitude: lon,
      photoUrl: photoUrl,           // ✅ Full Firebase Storage public URL
      photo_gridfs_id: photoUrl,    // legacy field kept for compatibility
      status: "Pending",
      upvotes: 0,
      impactScore: finalSeverity === 'High' ? 100 : finalSeverity === 'Medium' ? 50 : 10,
      severity: finalSeverity,
      ai_confidence: prediction.confidence,
      department: null,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // Award 10 points
    if (uid) {
      const userRef = db.collection("users").doc(uid.toString());
      await userRef.update({
        points: admin.firestore.FieldValue.increment(10)
      }).catch(e => console.error("Points update error:", e));
    }

    res.json({ success: true, complaintId: newId });
  } catch (err) {
    console.error("❌ Insert error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 📋 GET ALL COMPLAINTS
// ================================================
app.get("/complaints", async (req, res) => {
  try {
    const snapshot = await db.collection("complaints")
      .orderBy("upvotes", "desc")
      // Cannot double orderBy with a filter easily in base query so we sort memory or just rely on upvotes then created manually.
      .get();

    // We need to fetch user names manually (Like a SQL JOIN)
    const allUsersSnap = await db.collection("users").get();
    const userMap = {};
    allUsersSnap.forEach(doc => {
      userMap[doc.data().id] = doc.data();
    });

    const data = snapshot.docs.map(doc => {
      const row = doc.data();
      const user = userMap[row.user_id] || {};

      const createdStr = row.created_at ? new Date(row.created_at.toMillis()).toISOString() : null;

      // Feature 8: AI-Powered Public Impact Score
      let impact = (row.upvotes || 0) * 5;
      if (row.severity === 'High') impact += 50;
      else if (row.severity === 'Medium') impact += 30;
      else if (row.severity === 'Low') impact += 10;
      else impact += 5; // unprocessed

      return {
        id: row.id,
        category: row.category,
        description: row.description,
        address: row.place,
        latitude: row.latitude,
        longitude: row.longitude,
        created_at: createdStr,
        status: row.status,
        upvotes: row.upvotes || 0,
        severity: row.severity,
        impact_score: impact,
        ai_confidence: row.ai_confidence,
        department: row.department,
        user_id: row.user_id,
        reporter_name: user.name || "Anonymous",
        // ✅ Return full Firebase Storage URL directly (no prefix needed)
        photo_url: row.photoUrl || null
      };
    });

    // Sort heavily by AI Impact Score, then newest
    data.sort((a, b) => {
      if (b.impact_score === a.impact_score) {
        return new Date(b.created_at) - new Date(a.created_at);
      }
      return b.impact_score - a.impact_score;
    });

    res.json(data);
  } catch (err) {
    console.error("❌ Fetch error:", err);
    res.status(500).json([]);
  }
});

// ================================================
// 👍 UPVOTE (+2 pts to original reporter)
// ================================================
app.post("/upvote/:id", async (req, res) => {
  const compId = req.params.id;

  try {
    const compRef = db.collection("complaints").doc(compId.toString());
    const doc = await compRef.get();

    if (!doc.exists) return res.status(404).json({ success: false });

    // increment complaint upvote
    await compRef.update({
      upvotes: admin.firestore.FieldValue.increment(1)
    });

    // award 2 pts to reporter
    const row = doc.data();
    if (row.user_id) {
      await db.collection("users").doc(row.user_id.toString()).update({
        points: admin.firestore.FieldValue.increment(2)
      });
    }

    res.json({ success: true });
  } catch (err) {
    console.error("❌ Upvote error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 🏢 ASSIGN DEPARTMENT
// ================================================
app.put("/complaint/department/:id", async (req, res) => {
  try {
    await db.collection("complaints").doc(req.params.id).update({
      department: req.body.department
    });
    res.json({ success: true });
  } catch (err) {
    console.error("❌ Assign dept error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// ✅ MARK RESOLVED (+20 pts to reporter + FCM Push)
// ================================================
app.post("/complaint/resolve/:id", async (req, res) => {
  const compId = req.params.id;

  try {
    const compRef = db.collection("complaints").doc(compId.toString());
    const doc = await compRef.get();

    if (!doc.exists) return res.status(404).json({ success: false });

    await compRef.update({ status: 'Resolved' });

    const row = doc.data();
    if (row.user_id) {
      const userRef = db.collection("users").doc(row.user_id.toString());

      // 20 points
      await userRef.update({
        points: admin.firestore.FieldValue.increment(20)
      });

      // Trigger FCM Push notification if token exists
      const userDoc = await userRef.get();
      const token = userDoc.data()?.fcm_token;
      if (token && admin.apps.length > 0) {
        admin.messaging().send({
          token: token,
          notification: {
            title: "✅ Complaint Resolved!",
            body: "Good news! Admin has resolved your issue. You earned +20 points."
          }
        }).catch(err => console.error("FCM Send Error:", err));
      }
    }

    res.json({ success: true });
  } catch (err) {
    console.error("❌ Resolve error:", err);
    res.status(500).json({ success: false });
  }
});

// ================================================
// 🏆 LEADERBOARD (top 20 citizens by points)
// ================================================
app.get("/leaderboard", async (req, res) => {
  try {
    // 1. Get Top 20 Users by points directly from 'users'
    const usersSnap = await db.collection("users")
      .orderBy("points", "desc")
      .limit(20)
      .get();

    // Because we lack true JOINs in NoSQL, and we need total/resolved complaints counts,
    // we fetch ALL complaints to aggregate (fast enough for small-medium scales)
    const complSnap = await db.collection("complaints").get();

    // Build user -> metrics map
    const metrics = {};
    complSnap.forEach(doc => {
      const c = doc.data();
      if (c.user_id) {
        if (!metrics[c.user_id]) {
          metrics[c.user_id] = { total: 0, resolved: 0, upvotes: 0 };
        }
        metrics[c.user_id].total += 1;
        if (c.status === 'Resolved') metrics[c.user_id].resolved += 1;
        metrics[c.user_id].upvotes += (c.upvotes || 0);
      }
    });

    const leaderboard = usersSnap.docs.map(doc => {
      const u = doc.data();
      const m = metrics[u.id] || { total: 0, resolved: 0, upvotes: 0 };
      return {
        id: u.id,
        name: u.name,
        points: u.points || 0,
        total_complaints: m.total,
        resolved_complaints: m.resolved,
        total_upvotes: m.upvotes
      };
    });

    res.json(leaderboard);
  } catch (err) {
    console.error("❌ Leaderboard error:", err);
    res.status(500).json([]);
  }
});

// ===============================
// GAMIFICATION: GET USER POINTS
// ===============================
app.get("/user/:id/points", async (req, res) => {
  try {
    const doc = await db.collection("users").doc(req.params.id).get();
    if (!doc.exists) return res.status(404).json({ error: "User not found" });
    res.json({ points: doc.data().points || 0 });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// PUSH NOTIFICATIONS: UPDATE FCM TOKEN
// ===============================
app.post("/user/:id/fcm", async (req, res) => {
  try {
    await db.collection("users").doc(req.params.id).update({
      fcm_token: req.body.fcm_token
    });
    res.json({ message: "Token updated" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// API: ALERTS & ACCIDENTS
// ===============================
app.get("/alerts", async (req, res) => {
  try {
    const snap = await db.collection("alerts").orderBy("created_at", "desc").get();
    res.json(snap.docs.map(doc => ({ alertId: doc.id, ...doc.data() })));
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.post("/alerts", async (req, res) => {
  const { title, message, type, area } = req.body;
  if (!title || !message) return res.status(400).json({ success: false, message: "Title and message required" });

  try {
    const newId = await getNextId("alerts");
    await db.collection("alerts").doc(newId.toString()).set({
      title,
      message,
      type: type || "info",
      area: area || "",
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // FCM broadcast to all_users topic
    if (admin.apps.length > 0) {
      admin.messaging().send({
        topic: "all_users",
        notification: {
          title: `🚨 ${type === 'danger' ? 'Emergency Alert' : 'System Alert'}: ${title}`,
          body: message
        }
      }).catch(err => console.error("FCM Broadcast Error:", err));
    }

    res.json({ success: true, alertId: newId });
  } catch (err) {
    console.error("❌ Post alert error:", err);
    res.status(500).json({ success: false });
  }
});

app.post("/accidents", async (req, res) => {
  const { latitude, longitude } = req.body;
  if (latitude === undefined || longitude === undefined) return res.status(400).json({ error: "Missing coords" });
  try {
    const newId = await getNextId("accidents");
    await db.collection("accidents").doc(newId.toString()).set({
      accidentId: newId,
      latitude,
      longitude,
      created_at: admin.firestore.FieldValue.serverTimestamp()
    });

    // Broadcast Emergency Alert
    if (admin.apps.length > 0) {
      admin.messaging().send({
        topic: "all_users",
        notification: {
          title: "🚨 Emergency SOS Reported",
          body: "An accident was just reported near your area. Please stay safe and clear the roads for emergency services."
        }
      }).catch(err => console.error("FCM SOS Error:", err));
    }

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ===============================
// ACCIDENT BLACKSPOTS ALGORITHM
// ===============================
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
  var R = 6371; // Radius of the earth in km
  var dLat = deg2rad(lat2-lat1);  // deg2rad below
  var dLon = deg2rad(lon2-lon1); 
  var a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
    Math.sin(dLon/2) * Math.sin(dLon/2); 
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
  return R * c; 
}
function deg2rad(deg) { return deg * (Math.PI/180) }

app.get("/blackspots", async (req, res) => {
  try {
    const snap = await db.collection("accidents").get();
    const accidents = snap.docs.map(d => d.data());
    
    let clusters = [];
    let visited = new Set();
    
    // Spatial clustering: Group accidents within a 1km radius
    for(let i = 0; i < accidents.length; i++) {
       if(visited.has(i)) continue;
       let cluster = [accidents[i]];
       visited.add(i);
       
       for(let j = i + 1; j < accidents.length; j++) {
         if(visited.has(j)) continue;
         let dist = getDistanceFromLatLonInKm(
             parseFloat(accidents[i].latitude), parseFloat(accidents[i].longitude), 
             parseFloat(accidents[j].latitude), parseFloat(accidents[j].longitude)
         );
         if(dist <= 1.0) { // 1 km radius
            cluster.push(accidents[j]);
            visited.add(j);
         }
       }
       if(cluster.length >= 2) { 
          // Center point of the blackspot cluster
          let avgLat = cluster.reduce((sum, a) => sum + parseFloat(a.latitude), 0) / cluster.length;
          let avgLon = cluster.reduce((sum, a) => sum + parseFloat(a.longitude), 0) / cluster.length;
          clusters.push({ lat: avgLat, lng: avgLon, incidentCount: cluster.length });
       }
    }
    
    res.json(clusters);
  } catch(err) {
    res.status(500).json({ error: err.message });
  }
});

// ===============================
// SOCIAL MEDIA LISTENING
// ===============================
app.get("/social-listening", async (req, res) => {
  try {
    const query = "pothole OR accident OR drain OR waterlog OR garbage OR road";
    const url = `https://www.reddit.com/r/india/search.json?q=${encodeURIComponent(query)}&restrict_sr=on&sort=new&t=month&limit=15`;
    
    const response = await axios.get(url, { headers: { 'User-Agent': 'SCMS_Bot/1.0' } });
    
    const posts = response.data.data.children.map(child => {
       const d = child.data;
       const text = d.title.toLowerCase();
       const isHigh = text.includes('accident') || text.includes('flood') || text.includes('death') || text.includes('emergency');
       
       return {
          id: d.id,
          platform: 'Reddit',
          author: d.author,
          title: d.title,
          url: `https://reddit.com${d.permalink}`,
          score: d.score,
          comments: d.num_comments,
          severity: isHigh ? 'High' : 'Medium',
          created_utc: d.created_utc * 1000
       };
    });
    
    res.json(posts);
  } catch (err) {
    console.error("Social Listening fallback activated:", err.message);
    const fallbacks = [
        { id: "mock1", platform: "Reddit", author: "citizen_xyz", title: "Massive pothole on Ring Road causing accidents everyday", url: "#", score: 145, comments: 34, severity: "High", created_utc: Date.now() },
        { id: "mock2", platform: "Twitter/X", author: "mumbai_updates", title: "Waterlogging at Andheri subway again. BMC please fix!!", url: "#", score: 890, comments: 120, severity: "High", created_utc: Date.now() - 3600000 },
        { id: "mock3", platform: "Mastodon", author: "green_city", title: "Garbage unattended in Sector 4 for 3 straight days.", url: "#", score: 12, comments: 2, severity: "Medium", created_utc: Date.now() - 7200000 }
    ];
    res.json(fallbacks);
  }
});

// ===============================
// START SERVER
// ===============================
app.listen(3000, () => {
  console.log("🚀 SCMS Server running on http://localhost:3000 (Powered by Firebase Firestore)");
});
