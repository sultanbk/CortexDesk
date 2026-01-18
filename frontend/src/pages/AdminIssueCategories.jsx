import React, { useEffect, useState } from 'react';
import { getIssueCategories, createIssueCategory, updateIssueCategory, deleteIssueCategory } from '../services/issueCategoryApi';

export default function AdminIssueCategories() {
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ categoryCode: '', categoryName: '', description: '', slaHours: 24, isActive: true });

  const load = async () => {
    const data = await getIssueCategories();
    setCategories(data || []);
  };

  useEffect(() => { load(); }, []);

  const handleSave = async () => {
    const payload = { ...form };
    try {
      if (editing) {
        await updateIssueCategory(editing, payload);
      } else {
        await createIssueCategory(payload);
      }
      setForm({ categoryCode: '', categoryName: '', description: '', slaHours: 24, isActive: true });
      setEditing(null);
      await load();
    } catch (err) {
      console.error(err);
      alert('Failed to save category');
    }
  };

  const handleEdit = (c) => {
    setEditing(c.categoryId);
    setForm({ categoryCode: c.categoryCode || '', categoryName: c.categoryName || '', description: c.description || '', slaHours: c.slaHours || 24, isActive: c.isActive !== false });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this category?')) return;
    try { await deleteIssueCategory(id); await load(); } catch (e) { console.error(e); alert('Delete failed'); }
  };

  return (
    <div className="container mt-4">
      <h6 className="mb-3">Manage Issue Categories</h6>
      <div className="card mb-3">
        <div className="card-body">
          <div className="row g-2 mb-2">
            <div className="col-auto">
              <input 
                type="text"
                className="form-control form-control-sm" 
                placeholder="Code" 
                value={form.categoryCode} 
                onChange={(e) => setForm(f => ({ ...f, categoryCode: e.target.value }))} 
              />
            </div>
            <div className="col-auto">
              <input 
                type="text"
                className="form-control form-control-sm" 
                placeholder="Name" 
                value={form.categoryName} 
                onChange={(e) => setForm(f => ({ ...f, categoryName: e.target.value }))} 
                style={{ minWidth: '240px' }}
              />
            </div>
            <div className="col-auto">
              <input 
                type="number"
                className="form-control form-control-sm" 
                placeholder="SLA Hours" 
                value={form.slaHours} 
                onChange={(e) => setForm(f => ({ ...f, slaHours: Number(e.target.value) }))} 
                style={{ width: '120px' }}
              />
            </div>
            <div className="col-auto">
              <button className="btn btn-sm btn-primary" onClick={handleSave}>{editing ? 'Update' : 'Create'}</button>
              {editing && <button className="btn btn-sm btn-outline-secondary ms-2" onClick={() => { setEditing(null); setForm({ categoryCode: '', categoryName: '', description: '', slaHours: 24, isActive: true }); }}>Cancel</button>}
            </div>
          </div>
          <textarea 
            className="form-control form-control-sm"
            placeholder="Description" 
            value={form.description} 
            onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))} 
            rows="2"
          />
        </div>
      </div>

      <div className="card">
        <div className="table-responsive">
          <table className="table table-sm table-hover mb-0">
            <thead className="table-light">
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Description</th>
                <th>SLA (hrs)</th>
                <th>Active</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {categories.map(c => (
                <tr key={c.categoryId}>
                  <td>{c.categoryCode}</td>
                  <td>{c.categoryName}</td>
                  <td style={{ maxWidth: '360px' }}>{c.description}</td>
                  <td>{c.slaHours}</td>
                  <td>{c.isActive ? 'Yes' : 'No'}</td>
                  <td>
                    <button className="btn btn-sm btn-outline-primary me-2" onClick={() => handleEdit(c)}>Edit</button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(c.categoryId)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
