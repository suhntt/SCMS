const express = require("express");
const cors = require("cors");
const multer = require("multer");
const mysql = require("mysql2");
const path = require("path");

const app = express();
app.use(cors());
app.use(express.json());

// ✅ Serve images
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// ===============================
// MySQL Connection
// ===============================
const db = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "Password@321",
  database: "scms"
});

db.connect(err => {
  if (err) {
    console.error("❌ MySQL connection failed:", err);
    return;
  }
  console.log("✅ MySQL Connected");
});

// ===============================
// Multer Setup
// ===============================
const storage = multer.diskStorage({
  destination: "uploads/",
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname); // .jpg / .png
    const filename = Date.now() + ext;
    cb(null, filename);
  }
});

const upload = multer({ storage });

// =================================================
// SUBMIT COMPLAINT
// =================================================
app.post("/complaint", upload.single("photo"), (req, res) => {
  const { category, address, latitude, longitude, description } = req.body;

  const place =
    address && address.trim() !== ""
      ? address.trim()
      : "Address not available";

  const photoFilename = req.file ? req.file.filename : null;

  const sql = `
    INSERT INTO complaints
    (category, description, place, latitude, longitude, photo_gridfs_id, status, upvotes)
    VALUES (?, ?, ?, ?, ?, ?, 'Pending', 0)
  `;

  db.query(
    sql,
    [category, description, place, latitude, longitude, photoFilename],
    err => {
      if (err) {
        console.error("❌ Insert error:", err);
        return res.status(500).json({ success: false });
      }
      res.json({ success: true });
    }
  );
});

// =================================================
// GET ALL COMPLAINTS
// =================================================
app.get("/complaints", (req, res) => {
  const sql = `
    SELECT *
    FROM complaints
    ORDER BY upvotes DESC, created_at DESC
  `;

  db.query(sql, (err, rows) => {
    if (err) {
      console.error("❌ Fetch error:", err);
      return res.status(500).json([]);
    }

    const data = rows.map(row => ({
      id: row.id,
      category: row.category,
      description: row.description,
      address: row.place,
      latitude: row.latitude,
      longitude: row.longitude,
      created_at: row.created_at,
      status: row.status,
      upvotes: row.upvotes,
      photo_url: row.photo_gridfs_id
        ? `uploads/${row.photo_gridfs_id}`
        : null
    }));

    res.json(data);
  });
});

// =================================================
// UPVOTE
// =================================================
app.post("/upvote/:id", (req, res) => {
  db.query(
    "UPDATE complaints SET upvotes = upvotes + 1 WHERE id = ?",
    [req.params.id],
    err => {
      if (err) {
        console.error("❌ Upvote error:", err);
        return res.status(500).json({ success: false });
      }
      res.json({ success: true });
    }
  );
});

app.listen(3000, () => {
  console.log("🚀 Server running on http://localhost:3000");
});
