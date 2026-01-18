import { useState, useEffect, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faExclamationTriangle, faClock, faCheckCircle } from "@fortawesome/free-solid-svg-icons";
import StatusBadge from "../components/StatusBadge";
import { getCurrentUser } from "../auth/auth";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getAllTickets,
  assignTicket,
  resolveTicket,
  closeTicket,
  reopenTicket,
  pickTicket,
} from "../services/ticketApi";
import { autoAssignTicket } from "../services/ticketApi";
import { listAttachments, downloadAttachment } from "../services/ticketApi";

export default function TicketList({ role }) {
  const currentUser = getCurrentUser();
  const userId = currentUser?.userId;
  const [priorityMap, setPriorityMap] = useState({});
  const [resolutionMap, setResolutionMap] = useState({});
  const [reopenReasonMap, setReopenReasonMap] = useState({});
  const [tick, setTick] = useState(0);
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [attachments, setAttachments] = useState([]);
  const [attachmentsLoading, setAttachmentsLoading] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [expandAssignment, setExpandAssignment] = useState(false);
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [density, setDensity] = useState('compact');
  const filterRef = useRef(null);
  const [isFloating, setIsFloating] = useState(false);
  const [filterDims, setFilterDims] = useState({ left: 0, width: 0, height: 0 });

  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [view, setView] = useState("open"); // open | closed | all

  const { data: tickets = [], isLoading } = useQuery({ queryKey: ["tickets"], queryFn: getAllTickets, staleTime: 30_000, refetchInterval: 30_000 });

  const assignMutation = useMutation({ mutationFn: (payload) => assignTicket(payload), onSuccess: () => queryClient.invalidateQueries(["tickets"]) });
  const autoAssignMutation = useMutation({ 
    mutationFn: (ticketId) => autoAssignTicket(ticketId), 
    onSuccess: (data) => {
      queryClient.invalidateQueries(["tickets"]);
      if (selectedTicket && data) {
        setSelectedTicket(data);
        setExpandAssignment(true);
      }
    }
  });
  const resolveMutation = useMutation({ mutationFn: (payload) => resolveTicket(payload), onSuccess: () => queryClient.invalidateQueries(["tickets"]) });
  const pickMutation = useMutation({ mutationFn: (payload) => pickTicket(payload), onSuccess: () => queryClient.invalidateQueries(["tickets"]) });
  const closeMutation = useMutation({ mutationFn: (payload) => closeTicket(payload), onSuccess: () => queryClient.invalidateQueries(["tickets"]) });
  const reopenMutation = useMutation({ mutationFn: (payload) => reopenTicket(payload), onSuccess: () => queryClient.invalidateQueries(["tickets"]) });

  // keep countdowns updating in the UI even between refetches (1s for live timer)
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, []);

  const handlePriorityChange = (ticketId, priority) => {
    setPriorityMap((prev) => ({ ...prev, [ticketId]: priority }));
  };

  const handleAssign = (ticketId) => {
    if (!userId) {
      alert("User not loaded. Please login again.");
      return;
    }
    const payload = {
      ticketId: Number(ticketId),
      engineerId: 2,
      managerId: Number(userId),
      priority: (priorityMap[ticketId] || "HIGH").toUpperCase(),
    };
    assignMutation.mutate(payload, {
      onError: (err) => alert("Assignment Failed: " + (err?.message || err)),
    });
  };

  const handleAutoAssign = (ticketId) => {
    autoAssignMutation.mutate(ticketId, {
      onError: (err) => alert("Auto-assign failed: " + (err?.message || err)),
    });
  };

  const handlePick = (ticketId) => {
    if (!userId) {
      alert("User not loaded. Please login again.");
      return;
    }
    const payload = { ticketId: Number(ticketId), engineerId: Number(userId) };
    pickMutation.mutate(payload, {
      onError: (err) => {
        const serverMsg = err?.response?.data || err?.message || JSON.stringify(err);
        alert("Failed to pick ticket: " + serverMsg);
      },
    });
  };

  const handleClose = (ticketId) => {
    const payload = { ticketId, customerId: userId };
    closeMutation.mutate(payload, { onError: () => alert("Failed to close ticket.") });
  };

  const handleReopen = (ticketId) => {
    const reason = reopenReasonMap[ticketId];
    if (!reason || reason.trim() === "") {
      alert("Please provide a reason for reopening the ticket.");
      return;
    }
    const payload = { ticketId, customerId: userId, reopenReason: reason };
    reopenMutation.mutate(payload, {
      onSuccess: () => setReopenReasonMap((prev) => { const updated = { ...prev }; delete updated[ticketId]; return updated; }),
      onError: () => alert("Failed to reopen ticket."),
    });
  };

  const handleResolve = (ticketId) => {
    const ticket = tickets.find((t) => t.ticketId === ticketId);
    if (ticket?.status === "RESOLVED" || ticket?.status === "CLOSED") {
      alert("This ticket is already finalized.");
      return;
    }
    const payload = { ticketId: Number(ticketId), engineerId: userId, resolutionSummary: resolutionMap[ticketId] || "Issue resolved by engineer" };
    resolveMutation.mutate(payload, { onError: () => alert("Error: Likely trying to modify a finished ticket.") });
    setResolutionMap((prev) => { const updated = { ...prev }; delete updated[ticketId]; return updated; });
  };

  const baseFiltered = tickets.filter((t) => {
    if (role === "ENGINEER") return t.status !== "RESOLVED" && t.status !== "CLOSED";
    if (role === "MANAGER") return t.status !== "CLOSED";
    return true;
  });

  const closedOnly = tickets.filter((t) => t.status === "CLOSED");

  const displayedTickets = view === "closed" ? closedOnly : baseFiltered.filter((t) => view === "open" ? t.status !== "CLOSED" : true);

  // apply UI filters
  const filteredTickets = displayedTickets.filter((t) => {
    if (priorityFilter !== 'ALL' && (t.priority || 'NOT SET') !== priorityFilter) return false;
    if (statusFilter !== 'ALL' && t.status !== statusFilter) return false;
    if (searchTerm && searchTerm.trim() !== '') {
      const q = searchTerm.trim().toLowerCase();
      const matches = String(t.ticketId).includes(q) || (t.description || '').toLowerCase().includes(q) || (t.priority || '').toLowerCase().includes(q);
      if (!matches) return false;
    }
    return true;
  });

  // initialize filters from URL on first render
  useEffect(() => {
    const p = searchParams.get('priority');
    const s = searchParams.get('status');
    const q = searchParams.get('q');
    const v = searchParams.get('view');
    if (p) setPriorityFilter(p);
    if (s) setStatusFilter(s);
    if (q) setSearchTerm(q);
    if (v) setView(v);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // sync filters to URL
  useEffect(() => {
    const params = {};
    if (priorityFilter && priorityFilter !== 'ALL') params.priority = priorityFilter;
    if (statusFilter && statusFilter !== 'ALL') params.status = statusFilter;
    if (searchTerm && searchTerm.trim() !== '') params.q = searchTerm.trim();
    if (view && view !== 'open') params.view = view;
    setSearchParams(params, { replace: true });
  }, [priorityFilter, statusFilter, searchTerm, view, setSearchParams]);

  // floating toolbar behavior: removed for simplicity in Bootstrap


  const formatDate = (d) => {
    try {
      return d ? new Date(d).toLocaleString() : "-";
    } catch (e) {
      return "-";
    }
  };

  const remainingCountdown = (ticket) => {
    if (!ticket?.assignedByManager) return "Not started";
    if (!ticket?.slaDueTime) return "-";
    const diffMs = new Date(ticket.slaDueTime) - new Date();
    if (diffMs <= 0) return "00:00:00";
    const totalSecs = Math.floor(diffMs / 1000);
    const hrs = Math.floor(totalSecs / 3600);
    const mins = Math.floor((totalSecs % 3600) / 60);
    const secs = totalSecs % 60;
    const hh = String(hrs).padStart(2, "0");
    const mm = String(mins).padStart(2, "0");
    const ss = String(secs).padStart(2, "0");
    return `${hh}:${mm}:${ss}`;
  };

  const getSLAStatus = (ticket) => {
    if (!ticket?.assignedByManager || !ticket?.slaDueTime) return null;
    const diffMs = new Date(ticket.slaDueTime) - new Date();
    if (diffMs <= 0) return 'breached';
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 30) return 'alert';
    return 'on_track';
  };

  const getPriorityClass = (priority) => {
    const p = (priority || '').toUpperCase();
    if (p === 'HIGH') return 'border-danger';
    if (p === 'MEDIUM') return 'border-warning';
    if (p === 'LOW') return 'border-success';
    return 'border-secondary';
  };

  const openDetails = (t) => setSelectedTicket(t);
  const closeDetails = () => setSelectedTicket(null);

  // fetch attachments when dialog opens for a ticket
  useEffect(() => {
    let mounted = true;
    async function load() {
      setAttachments([]);
      setAttachmentsLoading(true);
      try {
        if (selectedTicket && selectedTicket.ticketId) {
          const list = await listAttachments(selectedTicket.ticketId);
          if (mounted) setAttachments(list || []);
        }
      } catch (e) {
        console.error('failed to load attachments', e);
      } finally {
        if (mounted) setAttachmentsLoading(false);
      }
    }
    if (selectedTicket) load();
    return () => { mounted = false; if (previewUrl) { URL.revokeObjectURL(previewUrl); setPreviewUrl(null); } };
  }, [selectedTicket]);

  if (isLoading) return <div className="container mt-4"><div className="spinner-border" role="status"><span className="visually-hidden">Loading...</span></div></div>;

  return (
    <div className="main-content-inner">
      {/* LEFT FILTER NAVBAR */}
      <div className="filters-nav">
        <h6 className="filters-nav-header mb-0">{currentUser?.role?.charAt(0).toUpperCase() + currentUser?.role?.slice(1).toLowerCase()} #{currentUser?.userId}</h6>
        <small className="filters-nav-subheader text-muted">{currentUser?.username}</small>
        <span className="filters-nav-title">Filters</span>
        
        {/* Priority Filter */}
        <div className="filters-nav-group">
          <label className="filter-label">Priority</label>
          <select 
            className="filter-select" 
            value={priorityFilter} 
            onChange={(e) => setPriorityFilter(e.target.value)}
            title="Filter by ticket priority"
          >
            <option value="ALL">All Priorities</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>
        </div>

        {/* Status Filter */}
        <div className="filters-nav-group">
          <label className="filter-label">Status</label>
          <select 
            className="filter-select" 
            value={statusFilter} 
            onChange={(e) => setStatusFilter(e.target.value)}
            title="Filter by ticket status"
          >
            <option value="ALL">All Statuses</option>
            <option value="NEW">NEW</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="RESOLVED">RESOLVED</option>
            <option value="CLOSED">CLOSED</option>
            <option value="REOPENED">REOPENED</option>
          </select>
        </div>

        {/* Search Input */}
        <div className="filters-nav-search">
          <input 
            type="text" 
            className="filter-input" 
            placeholder="Search tickets..." 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)}
            title="Search tickets by ID or description"
          />
        </div>

        {/* Clear Button */}
        <div className="filters-nav-actions">
          <button 
            className="filters-nav-clear"
            onClick={() => { setPriorityFilter('ALL'); setStatusFilter('ALL'); setSearchTerm(''); }}
            title="Clear all filters"
          >
            Clear
          </button>
        </div>
      </div>

      {/* CONTENT AREA */}
      <div className="content-wrapper">
        <div className="container-fluid tickets-container">
      <style>{`
        .card-hover { transition: transform 0.18s ease, box-shadow 0.18s ease; }
        .card-hover:hover { transform: translateY(-6px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
        .priority-high { border-left: 4px solid #dc3545; }
        .priority-medium { border-left: 4px solid #ffc107; }
        .priority-low { border-left: 4px solid #28a745; }
        .tickets-header { margin-bottom: 1rem; }
      `}</style>

      <div className="row mb-2 tickets-header">
        <div className="col-md-12">
          <div className="btn-group" role="group">
            <button type="button" className={`btn btn-sm ${view === "open" ? "btn-primary" : "btn-outline-primary"}`} onClick={() => setView("open")}>
              Open ({baseFiltered.filter(t => t.status !== 'CLOSED').length})
            </button>
            <button type="button" className={`btn btn-sm ${view === "closed" ? "btn-primary" : "btn-outline-primary"}`} onClick={() => setView("closed")}>
              Closed ({closedOnly.length})
            </button>
            <button type="button" className={`btn btn-sm ${view === "all" ? "btn-primary" : "btn-outline-primary"}`} onClick={() => setView("all")}>
              All ({tickets.length})
            </button>
          </div>
        </div>
      </div>

      <hr className="mb-3" style={{ borderColor: '#e8ecf1', opacity: 0.6 }} />

      {filteredTickets.length === 0 ? (
        <div className="alert alert-info">No tickets found. Try clearing filters or changing the view.</div>
      ) : (
        <div className="cards-grid">
          {filteredTickets.map((t) => (
            <div key={t.ticketId}>
              <div className="ticket-card" onClick={() => setSelectedTicket(t)}>
                {/* Header: ID and Priority Badge */}
                <div className="ticket-card-header">
                  <h5 className="ticket-id">#{t.ticketId}</h5>
                  <span className={
                    t.priority === 'HIGH' ? 'badge-priority badge-priority-high'
                    : t.priority === 'MEDIUM' ? 'badge-priority badge-priority-medium'
                    : t.priority === 'LOW' ? 'badge-priority badge-priority-low'
                    : 'badge-priority badge-priority-notset'
                  }>
                    {t.priority || 'NOT SET'}
                  </span>
                </div>

                {/* Description */}
                <p className="ticket-description">
                  {t.description && t.description.length > 85 
                    ? t.description.substring(0, 85) + '...' 
                    : t.description}
                </p>

                {/* Status Badge */}
                <div className="ticket-status-section">
                  <StatusBadge status={t.status} />
                </div>

                {/* Footer: Timer and SLA */}
                <div className="ticket-card-footer">
                  <div className="ticket-timer">
                    <span className="timer-label">SLA:</span>
                    <span className="timer-value">{remainingCountdown(t)}</span>
                  </div>
                  {getSLAStatus(t) && (
                    <span className={`badge sla-badge sla-${getSLAStatus(t)}`}>
                      {getSLAStatus(t) === 'breached' ? '⚠ BREACHED' 
                       : getSLAStatus(t) === 'alert' ? '⏱ ALERT' 
                       : '✓ ON TRACK'}
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedTicket && (
        <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Ticket #{selectedTicket?.ticketId}</h5>
                <button type="button" className="btn-close" onClick={() => setSelectedTicket(null)}></button>
              </div>
              <div className="modal-body">
                <div className="ticket-detail-description">
                  <p className="ticket-detail-text">{selectedTicket.description}</p>
                </div>
                <div className="row mb-3 align-items-center">
                  <div className="col-auto">
                    <span>Status: <StatusBadge status={selectedTicket.status} /></span>
                  </div>
                  <div className="col-auto">
                    <span className={
                      selectedTicket.priority === 'HIGH' ? 'badge-priority badge-priority-high'
                      : selectedTicket.priority === 'MEDIUM' ? 'badge-priority badge-priority-medium'
                      : selectedTicket.priority === 'LOW' ? 'badge-priority badge-priority-low'
                      : 'badge-priority badge-priority-notset'
                    }>
                      {selectedTicket.priority || 'NOT SET'}
                    </span>
                  </div>
                  <div className="col-auto">
                    <span>Timer: {remainingCountdown(selectedTicket)}</span>
                  </div>
                  {getSLAStatus(selectedTicket) && (
                    <div className="col-auto">
                      <span className={`badge sla-badge sla-${getSLAStatus(selectedTicket)}`}>
                        {getSLAStatus(selectedTicket) === 'breached' ? (
                          <>
                            <FontAwesomeIcon icon={faExclamationTriangle} style={{ marginRight: '0.5em' }} />
                            BREACHED
                          </>
                        ) : getSLAStatus(selectedTicket) === 'alert' ? (
                          <>
                            <FontAwesomeIcon icon={faClock} style={{ marginRight: '0.5em' }} />
                            ALERT
                          </>
                        ) : (
                          <>
                            <FontAwesomeIcon icon={faCheckCircle} style={{ marginRight: '0.5em' }} />
                            ON TRACK
                          </>
                        )}
                      </span>
                    </div>
                  )}
                </div>

                <hr />

                {selectedTicket.assignedByManager && (
                  <div className="ticket-assignment-info">
                    <button 
                      onClick={() => setExpandAssignment(!expandAssignment)}
                    >
                      <span>{expandAssignment ? '▼' : '▶'}</span>
                      Assignment Details
                    </button>
                    {expandAssignment && (
                      <div>
                        <div><strong>Assigned By:</strong> Manager #{typeof selectedTicket.assignedByManager === 'object' ? selectedTicket.assignedByManager.userId : selectedTicket.assignedByManager}</div>
                        {selectedTicket.assignedEngineer && <div><strong>Assigned To:</strong> Engineer #{typeof selectedTicket.assignedEngineer === 'object' ? selectedTicket.assignedEngineer.userId : selectedTicket.assignedEngineer}</div>}
                        {selectedTicket.assignedAt && <div><strong>Assigned Date:</strong> {new Date(selectedTicket.assignedAt).toLocaleString()}</div>}
                      </div>
                    )}
                  </div>
                )}

                <hr />

                {(attachmentsLoading || (!attachmentsLoading && attachments.length > 0)) && (
                  <>
                    <h6>Attachments</h6>
                    {attachmentsLoading && <small className="text-muted">Loading...</small>}
                    {!attachmentsLoading && attachments.length > 0 && (
                      <div>
                        {attachments.map((att) => (
                          <div key={att.attachmentId} className="d-flex justify-content-between align-items-center py-2 border-bottom">
                            <div>
                              <p className="mb-0">{att.fileName}</p>
                              <small className="text-muted">{att.contentType}</small>
                            </div>
                            <div className="btn-group btn-group-sm">
                              {att.contentType && att.contentType.startsWith('image') && (
                                <button className="btn btn-outline-primary" onClick={async () => {
                                  try {
                                    const blob = await downloadAttachment(att.attachmentId);
                                    const url = URL.createObjectURL(new Blob([blob]));
                                    window.open(url, '_blank');
                                  } catch (e) { console.error(e); }
                                }}>Preview</button>
                              )}
                              <button className="btn btn-outline-primary" onClick={async () => {
                                try {
                                  const blob = await downloadAttachment(att.attachmentId);
                                  const url = window.URL.createObjectURL(new Blob([blob]));
                                  const a = document.createElement('a');
                                  a.href = url;
                                  a.download = att.fileName || 'attachment';
                                  document.body.appendChild(a);
                                  a.click();
                                  a.remove();
                                  window.URL.revokeObjectURL(url);
                                } catch (e) { console.error('download failed', e); }
                              }}>Download</button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}

                {selectedTicket.resolutionSummary && (
                  <>
                    <hr />
                    <h6>Resolution Notes</h6>
                    <p className="text-muted">{selectedTicket.resolutionSummary}</p>
                  </>
                )}

                {role === 'MANAGER' && (selectedTicket.status === 'NEW' || selectedTicket.status === 'REOPENED') && (
                  <>
                    <hr />
                    <h6>Manager Actions</h6>
                    <div className="row g-2">
                      <div className="col-auto">
                        <select className="form-select form-select-sm" value={priorityMap[selectedTicket.ticketId] || selectedTicket.priority || 'HIGH'} onChange={(e) => setPriorityMap((prev) => ({ ...prev, [selectedTicket.ticketId]: e.target.value }))}>
                          <option value="LOW">LOW</option>
                          <option value="MEDIUM">MEDIUM</option>
                          <option value="HIGH">HIGH</option>
                        </select>
                      </div>
                      <div className="col-auto">
                        <button className="btn btn-sm btn-primary" onClick={() => { handleAssign(selectedTicket.ticketId); setSelectedTicket(null); }}>Assign & Start</button>
                      </div>
                      <div className="col-auto">
                        <button className="btn btn-sm btn-outline-primary" onClick={() => { handleAutoAssign(selectedTicket.ticketId); setSelectedTicket(null); }}>Auto-Assign</button>
                      </div>
                    </div>
                  </>
                )}

                {role === 'ENGINEER' && selectedTicket.status === 'ASSIGNED' && (
                  <>
                    <hr />
                    <h6>Engineer Actions</h6>
                    <button className="btn btn-sm btn-primary" onClick={() => { handlePick(selectedTicket.ticketId); setSelectedTicket(null); }}>Start Work</button>
                  </>
                )}

                {role === 'ENGINEER' && selectedTicket.status === 'IN_PROGRESS' && (
                  <>
                    <hr />
                    <h6>Engineer Actions</h6>
                    <div className="mb-2">
                      <textarea className="form-control form-control-sm" placeholder="Add resolution notes..." value={resolutionMap[selectedTicket.ticketId] || ''} rows="3" onChange={(e) => setResolutionMap((prev) => ({ ...prev, [selectedTicket.ticketId]: e.target.value }))} />
                    </div>
                    <button className="btn btn-sm btn-success" onClick={() => { handleResolve(selectedTicket.ticketId); setSelectedTicket(null); }}>Resolve Ticket</button>
                  </>
                )}

                {role === 'CUSTOMER' && selectedTicket.status === 'RESOLVED' && (
                  <>
                    <hr />
                    <h6>Customer Actions</h6>
                    <div className="mb-2">
                      <textarea className="form-control form-control-sm" placeholder="Reason for reopening" value={reopenReasonMap[selectedTicket.ticketId] || ''} rows="3" onChange={(e) => setReopenReasonMap((prev) => ({ ...prev, [selectedTicket.ticketId]: e.target.value }))} />
                    </div>
                    <button className="btn btn-sm btn-primary" onClick={() => { handleClose(selectedTicket.ticketId); setSelectedTicket(null); }}>Confirm & Close</button>
                    <button className="btn btn-sm btn-danger ms-2" disabled={!reopenReasonMap[selectedTicket.ticketId]?.trim()} onClick={() => { handleReopen(selectedTicket.ticketId); setSelectedTicket(null); }}>Reopen</button>
                  </>
                )}

                {role === 'CUSTOMER' && selectedTicket.status === 'CLOSED' && (
                  <>
                    <hr />
                    <div className="alert alert-success mb-0">Case Closed</div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setSelectedTicket(null)}>Close</button>
              </div>
            </div>
          </div>
        </div>
      )}
      </div>
    </div>
    </div>
  );
}
