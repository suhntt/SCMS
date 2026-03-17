import React, { useState, useEffect, useCallback } from 'react';
import { RouterProvider, createBrowserRouter, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, CircleMarker } from 'react-leaflet';
import axios from 'axios';
import 'leaflet/dist/leaflet.css';
import HeatmapLayer from './HeatmapLayer';
import {
  LayoutDashboard, AlertTriangle, Bot, Map, LogOut,
  CheckCircle2, Clock, XCircle, ThumbsUp, Users, BarChart3,
  RefreshCw, Flag, Building2, ChevronDown, Search, Filter, Bell, Rss
} from 'lucide-react';
import { Bar, Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title,
  Tooltip, Legend, ArcElement
} from 'chart.js';
import L from 'leaflet';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';
import './App.css';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement);

L.Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl });

const API = 'http://localhost:3000';

// ─────────────────────────────────────
// Sidebar
// ─────────────────────────────────────
function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  const links = [
    { icon: <LayoutDashboard size={18}/>, label: 'Dashboard', path: '/' },
    { icon: <Map size={18}/>,            label: 'Live Heatmap', path: '/heatmap' },
    { icon: <Flag size={18}/>,           label: 'Complaints',   path: '/complaints' },
    { icon: <AlertTriangle size={18}/>,  label: 'Alerts',       path: '/alerts' },
    { icon: <Rss size={18}/>,            label: 'Social Listening', path: '/social' },
    { icon: <Bot size={18}/>,            label: 'AI Processing',path: '/ai' },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="brand-icon">🏙️</span>
        <div>
          <div className="brand-title">SCMS Admin</div>
          <div className="brand-sub">Smart Civic Management</div>
        </div>
      </div>

      <nav className="sidebar-nav">
        {links.map(l => (
          <button
            key={l.path}
            className={`nav-link ${location.pathname === l.path ? 'active' : ''}`}
            onClick={() => navigate(l.path)}
          >
            {l.icon}
            <span>{l.label}</span>
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="live-dot-row">
          <span className="live-dot" />
          <span>System Online</span>
        </div>
      </div>
    </aside>
  );
}

// ─────────────────────────────────────
// Root Layout
// ─────────────────────────────────────
function Layout() {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

// ─────────────────────────────────────
// Stat Card
// ─────────────────────────────────────
function StatCard({ icon, label, value, color, sub }) {
  return (
    <div className="stat-card" style={{ '--accent': color }}>
      <div className="stat-icon" style={{ background: color + '22' }}>
        <span style={{ color }}>{icon}</span>
      </div>
      <div>
        <div className="stat-value">{value}</div>
        <div className="stat-label">{label}</div>
        {sub && <div className="stat-sub">{sub}</div>}
      </div>
    </div>
  );
}

// ─────────────────────────────────────
// Dashboard Page
// ─────────────────────────────────────
function DashboardPage() {
  const [complaints, setComplaints] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      axios.get(`${API}/complaints`),
      axios.get(`${API}/leaderboard`)
    ]).then(([c, u]) => {
      setComplaints(c.data);
      setUsers(u.data);
    }).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="center-loader"><div className="spinner"/></div>;

  const total    = complaints.length;
  const resolved = complaints.filter(c => c.status === 'Resolved').length;
  const pending  = complaints.filter(c => c.status === 'Pending').length;
  const high     = complaints.filter(c => c.severity === 'High').length;

  // Category chart
  const catCount = {};
  complaints.forEach(c => { catCount[c.category || 'Unknown'] = (catCount[c.category || 'Unknown'] || 0) + 1; });

  const barData = {
    labels: Object.keys(catCount),
    datasets: [{ label: 'Complaints', data: Object.values(catCount),
      backgroundColor: ['#3B82F6','#10B981','#F59E0B','#EF4444','#8B5CF6','#06B6D4'],
      borderRadius: 8, borderSkipped: false }]
  };

  const donutData = {
    labels: ['Resolved', 'Pending'],
    datasets: [{ data: [resolved, pending],
      backgroundColor: ['#10B981', '#F97316'], borderWidth: 0,
      hoverOffset: 6 }]
  };

  const recent = [...complaints].sort((a,b) => new Date(b.created_at) - new Date(a.created_at)).slice(0,5);

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Dashboard Overview</h1>
        <span className="page-sub">Real-time civic management stats</span>
      </div>

      <div className="stats-grid">
        <StatCard icon={<BarChart3 size={22}/>} label="Total Complaints" value={total}   color="#3B82F6" />
        <StatCard icon={<CheckCircle2 size={22}/>} label="Resolved"     value={resolved} color="#10B981" sub={`${total ? Math.round(resolved/total*100) : 0}% resolution rate`} />
        <StatCard icon={<Clock size={22}/>}     label="Pending"         value={pending}  color="#F97316" />
        <StatCard icon={<AlertTriangle size={22}/>} label="High Severity" value={high}  color="#EF4444" />
        <StatCard icon={<Users size={22}/>}     label="Registered Users" value={users.length} color="#8B5CF6" />
      </div>

      <div className="chart-row">
        <div className="chart-card">
          <h3 className="chart-title">Complaints by Category</h3>
          <Bar data={barData} options={{ responsive:true, plugins:{ legend:{ display:false } },
            scales:{ x:{ ticks:{ color:'#94A3B8' }, grid:{ color:'#1E3A5F33' } },
                     y:{ ticks:{ color:'#94A3B8' }, grid:{ color:'#1E3A5F33' } } } }} />
        </div>
        <div className="chart-card chart-card-sm">
          <h3 className="chart-title">Resolution Status</h3>
          <div className="donut-wrap">
            <Doughnut data={donutData} options={{ responsive:true, cutout:'70%',
              plugins:{ legend:{ labels:{ color:'#CBD5E1' } } } }} />
          </div>
        </div>
      </div>

      <div className="table-card">
        <h3 className="chart-title">Recent Complaints</h3>
        <table className="data-table">
          <thead>
            <tr><th>#</th><th>Category</th><th>Reporter</th><th>Address</th><th>Severity</th><th>Status</th></tr>
          </thead>
          <tbody>
            {recent.map(c => (
              <tr key={c.id}>
                <td className="td-muted">#{c.id}</td>
                <td><span className="badge badge-blue">{c.category || '—'}</span></td>
                <td>{c.reporter_name || 'Anon'}</td>
                <td className="td-muted">{c.address || '—'}</td>
                <td><SeverityBadge sev={c.severity} /></td>
                <td><StatusBadge status={c.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─────────────────────────────────────
// Live Heatmap Page
// ─────────────────────────────────────
function HeatmapPage() {
  const [complaints, setComplaints] = useState([]);
  const [blackspots, setBlackspots] = useState([]);
  const [loading, setLoading] = useState(true);
  const center = [20.5937, 78.9629];

  const fetch = useCallback(() => {
    Promise.all([
      axios.get(`${API}/complaints`),
      axios.get(`${API}/blackspots`)
    ]).then(([compRes, blackspotRes]) => {
      setComplaints(compRes.data.filter(c => c.latitude && c.longitude));
      setBlackspots(blackspotRes.data);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    fetch();
    const iv = setInterval(fetch, 10000);
    return () => clearInterval(iv);
  }, [fetch]);

  return (
    <div className="page heatmap-page">
      <div className="page-header">
        <h1 className="page-title">Live Civic Heatmap</h1>
        <div className="live-pill"><span className="live-dot"/> Live — auto-refreshes every 10s</div>
      </div>
      <div className="map-container">
        {loading ? (
          <div className="center-loader"><div className="spinner"/></div>
        ) : (
          <MapContainer
            center={complaints.length > 0 ? [parseFloat(complaints[0].latitude), parseFloat(complaints[0].longitude)] : center}
            zoom={13}
            style={{ height: '100%', width: '100%' }}
            zoomControl={true}
          >
            <TileLayer
              attribution='&copy; <a href="https://osm.org/copyright">OpenStreetMap</a>'
              url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
            />
            <HeatmapLayer points={complaints} />
            {complaints.map(c => (
              <Marker key={c.id} position={[parseFloat(c.latitude), parseFloat(c.longitude)]}>
                <Popup>
                  <div style={{ minWidth: 180 }}>
                    <strong>{c.category}</strong><br/>
                    <span style={{ fontSize: 12, color: '#64748B' }}>By: {c.reporter_name}</span>
                    <p style={{ margin: '6px 0', fontSize: 13 }}>{c.description}</p>
                    <span className={`badge ${c.severity === 'High' ? 'badge-red' : 'badge-orange'}`}>
                      AI: {c.severity || 'Unprocessed'}
                    </span>
                  </div>
                </Popup>
              </Marker>
            ))}
            
            {/* BLACKSPOTS */}
            {blackspots.map((spot, idx) => (
              <CircleMarker
                key={`bs-${idx}`}
                center={[spot.lat, spot.lng]}
                radius={24}
                pathOptions={{ color: '#EF4444', fillColor: '#EF4444', fillOpacity: 0.4, weight: 2 }}
              >
                <Popup>
                  <strong style={{ color: '#EF4444' }}>🚨 Accident Blackspot</strong>
                  <p style={{ margin: '4px 0 0', fontSize: 13 }}>
                    {spot.incidentCount} reported emergency SOS incidents near this 1km radius cluster.
                  </p>
                </Popup>
              </CircleMarker>
            ))}
          </MapContainer>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────
// Helpers
// ─────────────────────────────────────
function SeverityBadge({ sev }) {
  const map = { High: 'badge-red', Medium: 'badge-orange', Low: 'badge-green' };
  return <span className={`badge ${map[sev] || 'badge-blue'}`}>{sev || '—'}</span>;
}
function StatusBadge({ status }) {
  const map = { Resolved: 'badge-green', Pending: 'badge-orange', Rejected: 'badge-red' };
  return <span className={`badge ${map[status] || 'badge-blue'}`}>{status || '—'}</span>;
}

const DEPARTMENTS = ['PWD (Roads & Infra)', 'Sanitation Dept', 'Police / Emergency',
  'Traffic Authority', 'Environmental Board', 'Noise Control'];

// ─────────────────────────────────────
// Complaints Management Page
// ─────────────────────────────────────
function ComplaintsPage() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('All');
  const [actionLoading, setActionLoading] = useState({});
  const [deptModal, setDeptModal] = useState(null); // { id, current }
  const [selectedDept, setSelectedDept] = useState('');
  const [toast, setToast] = useState('');

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

  const load = useCallback(() => {
    axios.get(`${API}/complaints`).then(r => { setComplaints(r.data); setLoading(false); });
  }, []);
  useEffect(() => { load(); }, [load]);

  const resolve = async (id) => {
    setActionLoading(p => ({ ...p, [id]: 'resolving' }));
    try {
      await axios.post(`${API}/complaint/resolve/${id}`);
      showToast('✅ Complaint resolved! Reporter earned +20 pts.');
      load();
    } catch { showToast('❌ Failed to resolve.'); }
    setActionLoading(p => ({ ...p, [id]: null }));
  };

  const assignDept = async () => {
    if (!selectedDept) return;
    setActionLoading(p => ({ ...p, [deptModal.id]: 'dept' }));
    try {
      await axios.put(`${API}/complaint/department/${deptModal.id}`, { department: selectedDept });
      showToast(`🏢 Assigned to ${selectedDept}`);
      setDeptModal(null);
      load();
    } catch { showToast('❌ Failed to assign department.'); }
    setActionLoading(p => ({ ...p, [deptModal.id]: null }));
  };

  const filtered = complaints.filter(c => {
    const matchSearch = !search || [c.category, c.description, c.reporter_name, c.address]
      .some(f => f?.toLowerCase().includes(search.toLowerCase()));
    const matchFilter = filter === 'All' || c.status === filter || c.severity === filter;
    return matchSearch && matchFilter;
  });

  if (loading) return <div className="center-loader"><div className="spinner"/></div>;

  return (
    <div className="page">
      {toast && <div className="toast">{toast}</div>}

      {deptModal && (
        <div className="modal-overlay" onClick={() => setDeptModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Assign Department</h3>
            <p className="modal-sub">Complaint #{deptModal.id} — {deptModal.category}</p>
            <div className="dept-list">
              {DEPARTMENTS.map(d => (
                <button
                  key={d}
                  className={`dept-btn ${selectedDept === d ? 'active' : ''}`}
                  onClick={() => setSelectedDept(d)}
                >{d}</button>
              ))}
            </div>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeptModal(null)}>Cancel</button>
              <button className="btn-primary" onClick={assignDept} disabled={!selectedDept}>Assign</button>
            </div>
          </div>
        </div>
      )}

      <div className="page-header">
        <h1 className="page-title">Complaints Management</h1>
        <button className="btn-icon" onClick={load}><RefreshCw size={16}/></button>
      </div>

      <div className="toolbar">
        <div className="search-box">
          <Search size={15} style={{ color: '#64748B' }}/>
          <input
            placeholder="Search complaints…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="filter-row">
          {['All','Pending','Resolved','High','Medium','Low'].map(f => (
            <button
              key={f}
              className={`filter-btn ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >{f}</button>
          ))}
        </div>
      </div>

      <div className="table-card">
        <div className="table-meta">{filtered.length} of {complaints.length} complaints</div>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>#</th><th>Category</th><th>Description</th>
                <th>Reporter</th><th>Address</th>
                <th>Severity</th><th>Impact Score</th><th>Department</th><th>Status</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(c => (
                <tr key={c.id}>
                  <td className="td-muted">#{c.id}</td>
                  <td><span className="badge badge-blue">{c.category || '—'}</span></td>
                  <td className="td-desc">{c.description?.slice(0, 60) || '—'}{c.description?.length > 60 ? '…' : ''}</td>
                  <td>{c.reporter_name || 'Anon'}</td>
                  <td className="td-muted">{c.address?.slice(0, 30) || '—'}</td>
                  <td><SeverityBadge sev={c.severity}/></td>
                  <td><span className="badge" style={{ background: 'linear-gradient(135deg, #a855f7, #7e22ce)', color: 'white' }}>{c.impact_score || 0} pts</span></td>
                  <td className="td-muted">{c.department || <span className="unassigned">Unassigned</span>}</td>
                  <td><StatusBadge status={c.status}/></td>
                  <td>
                    <div className="action-btns">
                      {c.status !== 'Resolved' && (
                        <button
                          className="action-btn action-btn-green"
                          onClick={() => resolve(c.id)}
                          disabled={actionLoading[c.id]}
                          title="Mark Resolved"
                        >
                          {actionLoading[c.id] === 'resolving' ? '…' : <CheckCircle2 size={14}/>}
                        </button>
                      )}
                      <button
                        className="action-btn action-btn-blue"
                        onClick={() => { setDeptModal({ id: c.id, category: c.category }); setSelectedDept(c.department || ''); }}
                        title="Assign Department"
                      >
                        <Building2 size={14}/>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filtered.length === 0 && <div className="empty-state">No complaints match your filter.</div>}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────
// Alerts Page
// ─────────────────────────────────────
function AlertsPage() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [newAlert, setNewAlert] = useState({ title: '', message: '', type: 'info', area: '' });
  const [toast, setToast] = useState('');

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000); };

  const loadAlerts = useCallback(() => {
    axios.get(`${API}/alerts`).then(r => setAlerts(r.data)).catch(() => setAlerts([])).finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadAlerts(); }, [loadAlerts]);

  const handleCreateAlert = async () => {
    if (!newAlert.title || !newAlert.message) return showToast('Title and message are required');
    try {
      await axios.post(`${API}/alerts`, newAlert);
      showToast('✅ Alert broadcasted successfully!');
      setShowModal(false);
      setNewAlert({ title: '', message: '', type: 'info', area: '' });
      loadAlerts();
    } catch { showToast('❌ Failed to create alert'); }
  };

  if (loading) return <div className="center-loader"><div className="spinner"/></div>;

  const typeColor = { danger: '#EF4444', warning: '#F97316', info: '#3B82F6' };

  return (
    <div className="page">
      {toast && <div className="toast">{toast}</div>}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Create Emergency Alert</h3>
            <p className="modal-sub">This will broadcast to all active mobile users.</p>
            
            <div className="form-group" style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <label style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 'bold' }}>Alert Title <span style={{color:'#EF4444'}}>*</span></label>
              <input value={newAlert.title} onChange={e => setNewAlert({...newAlert, title: e.target.value})} placeholder="e.g. Heavy Rainfall Warning" style={{ width: '100%', padding: '10px', background: '#0F1B2D', border: '1px solid #1E3A5F', borderRadius: '8px', color: '#fff' }} />
              
              <label style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 'bold' }}>Message <span style={{color:'#EF4444'}}>*</span></label>
              <textarea value={newAlert.message} onChange={e => setNewAlert({...newAlert, message: e.target.value})} placeholder="Provide detailed information..." style={{ width: '100%', padding: '10px', background: '#0F1B2D', border: '1px solid #1E3A5F', borderRadius: '8px', color: '#fff', minHeight: '80px', resize: 'vertical' }} />
              
              <label style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 'bold' }}>Severity Type</label>
              <select value={newAlert.type} onChange={e => setNewAlert({...newAlert, type: e.target.value})} style={{ width: '100%', padding: '10px', background: '#0F1B2D', border: '1px solid #1E3A5F', borderRadius: '8px', color: '#fff' }}>
                <option value="info">Info (Blue)</option>
                <option value="warning">Warning (Orange)</option>
                <option value="danger">Danger (Red)</option>
              </select>

              <label style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 'bold' }}>Affected Area (Optional)</label>
              <input value={newAlert.area} onChange={e => setNewAlert({...newAlert, area: e.target.value})} placeholder="e.g. North District" style={{ width: '100%', padding: '10px', background: '#0F1B2D', border: '1px solid #1E3A5F', borderRadius: '8px', color: '#fff', marginBottom: '4px' }} />
            </div>
            
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
              <button className="btn-primary" onClick={handleCreateAlert} style={{ background: '#EF4444' }}>Broadcast Alert</button>
            </div>
          </div>
        </div>
      )}

      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1 className="page-title">Alerts & Incidents</h1>
          <span className="page-sub">{alerts.length} active alert{alerts.length !== 1 ? 's' : ''}</span>
        </div>
        <button className="btn-primary" onClick={() => setShowModal(true)} style={{ background: '#EF4444', height: 'fit-content' }}>
          + New Alert
        </button>
      </div>

      {alerts.length === 0 ? (
        <div className="empty-page">
          <div className="empty-icon">✅</div>
          <h3>No Active Alerts</h3>
          <p>The system is operating normally. No civic alerts are currently active.</p>
        </div>
      ) : (
        <div className="alerts-grid">
          {alerts.map(a => (
            <div
              key={a.alertId}
              className="alert-card"
              style={{ '--alert-color': typeColor[a.type] || '#3B82F6' }}
            >
              <div className="alert-header">
                <Bell size={18} style={{ color: typeColor[a.type] || '#3B82F6' }}/>
                <span className={`badge badge-${a.type === 'danger' ? 'red' : a.type === 'warning' ? 'orange' : 'blue'}`}>
                  {a.type || 'info'}
                </span>
              </div>
              <h4 className="alert-title">{a.title || 'Alert'}</h4>
              <p className="alert-msg">{a.message || '—'}</p>
              {a.area && <div className="alert-meta">📍 {a.area}</div>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────
// AI Processing Page
// ─────────────────────────────────────
function AIPage() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios.get(`${API}/complaints`).then(r => setComplaints(r.data)).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="center-loader"><div className="spinner"/></div>;

  const withAI   = complaints.filter(c => c.ai_confidence > 0);
  const avgConf  = withAI.length
    ? (withAI.reduce((s, c) => s + (c.ai_confidence || 0), 0) / withAI.length * 100).toFixed(1)
    : 0;

  const sevDist = { High: 0, Medium: 0, Low: 0 };
  complaints.forEach(c => { if (c.severity) sevDist[c.severity] = (sevDist[c.severity] || 0) + 1; });

  const catMap = {};
  complaints.forEach(c => { catMap[c.category || 'Unknown'] = (catMap[c.category||'Unknown']||0)+1; });

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">AI Processing Dashboard</h1>
        <span className="page-sub">Zero-shot MobileBERT classifier — runs locally</span>
      </div>

      <div className="stats-grid">
        <StatCard icon={<Bot size={22}/>}           label="AI Processed"    value={withAI.length}   color="#8B5CF6" />
        <StatCard icon={<BarChart3 size={22}/>}     label="Avg Confidence"  value={`${avgConf}%`}   color="#3B82F6" />
        <StatCard icon={<AlertTriangle size={22}/>} label="High Severity"   value={sevDist.High}    color="#EF4444" />
        <StatCard icon={<Clock size={22}/>}         label="Medium Severity" value={sevDist.Medium}  color="#F97316" />
        <StatCard icon={<CheckCircle2 size={22}/>}  label="Low Severity"    value={sevDist.Low}     color="#10B981" />
      </div>

      <div className="table-card">
        <h3 className="chart-title">AI Classification Results</h3>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>#</th><th>Description</th><th>AI Category</th>
                <th>Severity</th><th>Confidence</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              {complaints.map(c => (
                <tr key={c.id}>
                  <td className="td-muted">#{c.id}</td>
                  <td className="td-desc">{c.description?.slice(0,70) || '—'}</td>
                  <td><span className="badge badge-purple">{c.category || '—'}</span></td>
                  <td><SeverityBadge sev={c.severity}/></td>
                  <td>
                    <div className="conf-bar-wrap">
                      <div className="conf-bar" style={{ width: `${((c.ai_confidence||0)*100).toFixed(0)}%` }}/>
                      <span className="conf-label">{((c.ai_confidence||0)*100).toFixed(0)}%</span>
                    </div>
                  </td>
                  <td><StatusBadge status={c.status}/></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────
// Social Media Listening Page
// ─────────────────────────────────────
function SocialMediaPage() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios.get(`${API}/social-listening`)
      .then(r => setPosts(r.data))
      .catch(e => console.error("Social Error", e))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page social-page">
      <div className="page-header">
        <h1 className="page-title">Social Media Listening</h1>
        <div className="live-pill"><span className="live-dot"/> Live Network</div>
      </div>
      <p style={{color:'#64748B', marginBottom: '20px', fontSize: '14px'}}>
        Early detection of civic issues trending on social networks (Reddit API) before official complaints arrive.
      </p>
      
      {loading ? <div className="center-loader"><div className="spinner"/></div> : (
        <div className="table-card table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Platform</th><th>Title & Link</th><th>Author</th>
                <th>Score</th><th>Comments</th><th>AI Severity</th>
              </tr>
            </thead>
            <tbody>
              {posts.map(p => (
                <tr key={p.id}>
                  <td><span className="badge badge-blue">{p.platform}</span></td>
                  <td className="td-desc"><a href={p.url} target="_blank" rel="noreferrer" style={{color:'#fff', fontWeight:500, textDecoration:'none'}}>{p.title}</a></td>
                  <td className="td-muted">u/{p.author}</td>
                  <td className="td-muted">{p.score}</td>
                  <td className="td-muted">{p.comments}</td>
                  <td><SeverityBadge sev={p.severity}/></td>
                </tr>
              ))}
              {posts.length === 0 && <tr><td colSpan="6" style={{textAlign:'center', padding:40, color:'#64748B'}}>No trending issues today.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────
// Router
// ─────────────────────────────────────
const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true,         element: <DashboardPage /> },
      { path: 'heatmap',     element: <HeatmapPage /> },
      { path: 'complaints',  element: <ComplaintsPage /> },
      { path: 'alerts',      element: <AlertsPage /> },
      { path: 'social',      element: <SocialMediaPage /> },
      { path: 'ai',          element: <AIPage /> },
    ]
  }
]);

export default function App() {
  return <RouterProvider router={router} />;
}
