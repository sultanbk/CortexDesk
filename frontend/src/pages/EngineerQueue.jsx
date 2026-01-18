import React, { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getCurrentUser } from "../auth/auth";
import { getEngineerQueue, pickTicket, listAttachments, downloadAttachment, getTicketHistory, resolveTicket } from "../services/ticketApi";
import StatusBadge from "../components/StatusBadge";

export default function EngineerQueue() {
  const user = getCurrentUser();
  const userId = user?.userId;
  const queryClient = useQueryClient();
  const [showClosed, setShowClosed] = useState(false);
  
  // Fetch all tickets for this engineer (includes active and closed)
  const { data: allTickets = [], isLoading } = useQuery({
    queryKey: ["engineerQueue", userId],
    queryFn: () => getEngineerQueue(userId),
    enabled: !!userId,
    staleTime: 15_000,
  });

  const pickMutation = useMutation({ mutationFn: (payload) => pickTicket(payload), onSuccess: () => { queryClient.invalidateQueries(["engineerQueue"]); queryClient.invalidateQueries(["tickets"]); } });
  const resolveMutation = useMutation({ mutationFn: (payload) => resolveTicket(payload), onSuccess: () => { queryClient.invalidateQueries(["engineerQueue"]); queryClient.invalidateQueries(["tickets"]); } });

  const [selectedTicket, setSelectedTicket] = useState(null);
  const [attachments, setAttachments] = useState([]);
  const [attachmentsLoading, setAttachmentsLoading] = useState(false);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [resolutionText, setResolutionText] = useState("");

  const handlePick = (ticketId) => {
    if (!userId) return alert("User not loaded");
    pickMutation.mutate({ ticketId: Number(ticketId), engineerId: Number(userId) }, { onError: (err) => alert(err?.response?.data || err?.message || 'Pick failed') });
  };

  useEffect(() => {
    let mounted = true;
    async function loadDetails() {
      if (!selectedTicket) return;
      setAttachments([]); setHistory([]);
      setAttachmentsLoading(true); setHistoryLoading(true);
      try {
        const atts = await listAttachments(selectedTicket.ticketId);
        if (mounted) setAttachments(atts || []);
      } catch (e) { console.error('failed to load attachments', e); }
      finally { if (mounted) setAttachmentsLoading(false); }

      try {
        const h = await getTicketHistory(selectedTicket.ticketId);
        if (mounted) setHistory(h || []);
      } catch (e) { console.error('failed to load history', e); }
      finally { if (mounted) setHistoryLoading(false); }
    }
    loadDetails();
    return () => { mounted = false; };
  }, [selectedTicket]);

  const handleResolve = (ticketId) => {
    if (!resolutionText || resolutionText.trim().length < 5) return alert('Please provide resolution notes');
    resolveMutation.mutate({ ticketId: Number(ticketId), engineerId: Number(userId), resolutionSummary: resolutionText }, { onError: (err) => alert(err?.response?.data || err?.message || 'Resolve failed'), onSuccess: () => setSelectedTicket(null) });
  };

  const remaining = (t) => {
    if (!t?.assignedByManager) return 'Not started';
    if (!t?.slaDueTime) return '-';
    const diffMs = new Date(t.slaDueTime) - new Date();
    if (diffMs <= 0) return '00:00:00';
    const totalSecs = Math.floor(diffMs / 1000);
    const hrs = Math.floor(totalSecs / 3600);
    const mins = Math.floor((totalSecs % 3600) / 60);
    const secs = totalSecs % 60;
    return `${String(hrs).padStart(2,'0')}:${String(mins).padStart(2,'0')}:${String(secs).padStart(2,'0')}`;
  };

  if (isLoading) return <div className="container mt-4"><div className="spinner-border" role="status"><span className="visually-hidden">Loading...</span></div></div>;

  // Debug: log all tickets and their statuses
  console.log('All tickets:', allTickets);
  console.log('Unique statuses:', [...new Set(allTickets.map(t => t.status))]);

  // Filter: Show closed/resolved tickets OR if not many active tickets, show all
  let filteredTickets;
  if (showClosed) {
    filteredTickets = allTickets.filter(t => 
      t.status === 'CLOSED' || t.status === 'RESOLVED' || 
      t.status?.toUpperCase() === 'CLOSED' || t.status?.toUpperCase() === 'RESOLVED'
    );
  } else {
    filteredTickets = allTickets.filter(t => 
      t.status !== 'CLOSED' && t.status !== 'RESOLVED' &&
      t.status?.toUpperCase() !== 'CLOSED' && t.status?.toUpperCase() !== 'RESOLVED'
    );
  }

  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h6 className="mb-0">{showClosed ? 'Closed Tickets' : 'Your Queue'}</h6>
        <button 
          className={`btn btn-sm ${showClosed ? 'btn-secondary' : 'btn-outline-secondary'}`}
          onClick={() => setShowClosed(!showClosed)}
        >
          {showClosed ? 'Active Tickets' : 'Closed Tickets'}
        </button>
      </div>
      {filteredTickets.length === 0 && <p className="text-muted">No {showClosed ? 'closed' : 'active'} tickets.</p>}
      <div className="row g-3">
        {filteredTickets.map((t) => (
          <div className="col-12 col-sm-6 col-md-4" key={t.ticketId}>
            <div className="card h-100" style={{ cursor: 'pointer' }} onClick={() => setSelectedTicket(t)}>
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start mb-2">
                  <div>
                    <h6 className="card-subtitle mb-2">#{t.ticketId} • {t.priority || 'NOT SET'}</h6>
                    <p className="card-text text-muted small" style={{ maxHeight: '40px', overflow: 'hidden' }}>{t.description}</p>
                  </div>
                  <div>
                    <StatusBadge status={t.status} />
                  </div>
                </div>
                <small className="text-muted">SLA: {remaining(t)}</small>
              </div>
              <div className="card-footer">
                <button 
                  className="btn btn-sm btn-primary" 
                  onClick={() => handlePick(t.ticketId)}
                  disabled={t.status === 'IN_PROGRESS' || t.status === 'CLOSED'}
                >
                  Start Work
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {selectedTicket && (
        <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Ticket #{selectedTicket?.ticketId}</h5>
                <button type="button" className="btn-close" onClick={() => setSelectedTicket(null)}></button>
              </div>
              <div className="modal-body">
                {selectedTicket && (
                  <div>
                    <h6 className="mb-3">{selectedTicket.description}</h6>
                    <div className="row mb-3">
                      <div className="col-auto">
                        <small className="text-muted">Status: <strong>{selectedTicket.status}</strong></small>
                      </div>
                      <div className="col-auto">
                        <small className="text-muted">Priority: <strong>{selectedTicket.priority || 'NOT SET'}</strong></small>
                      </div>
                      <div className="col-auto">
                        <small className="text-muted">SLA: <strong>{remaining(selectedTicket)}</strong></small>
                      </div>
                    </div>

                    <hr />

                    <h6>Attachments</h6>
                    {attachmentsLoading && <small className="text-muted">Loading...</small>}
                    {!attachmentsLoading && attachments.length === 0 && <small className="text-muted">No attachments</small>}
                    {!attachmentsLoading && attachments.map(att => (
                      <div key={att.attachmentId} className="d-flex justify-content-between align-items-center py-2 border-bottom">
                        <div>
                          <p className="mb-0">{att.fileName}</p>
                          <small className="text-muted">{att.contentType}</small>
                        </div>
                        <button 
                          className="btn btn-sm btn-outline-primary"
                          onClick={async () => {
                            try {
                              const blob = await downloadAttachment(att.attachmentId);
                              const url = window.URL.createObjectURL(new Blob([blob]));
                              window.open(url, '_blank');
                            } catch (e) { console.error(e); }
                          }}
                        >
                          Preview
                        </button>
                      </div>
                    ))}

                    <hr />

                    <h6>History</h6>
                    {historyLoading && <small className="text-muted">Loading history...</small>}
                    {!historyLoading && history.length === 0 && <small className="text-muted">No history</small>}
                    {!historyLoading && history.map((h, idx) => (
                      <div key={`history-${h.ticketHistoryId || h.id || idx}`} className="py-2 border-bottom">
                        <p className="mb-1"><strong>{h.newStatus}</strong> by {h.changedBy?.name || h.changedBy?.username || 'system'}</p>
                        <small className="text-muted">{new Date(h.changedAt).toLocaleString()}</small>
                      </div>
                    ))}

                    <hr />

                    {selectedTicket.status === 'IN_PROGRESS' && (
                      <div>
                        <h6>Resolve Ticket</h6>
                        <textarea 
                          className="form-control"
                          placeholder="Resolution notes" 
                          value={resolutionText} 
                          onChange={(e) => setResolutionText(e.target.value)}
                          rows="3"
                        />
                      </div>
                    )}
                  </div>
                )}
              </div>
              <div className="modal-footer">
                {selectedTicket?.status === 'ASSIGNED' && (
                  <button className="btn btn-primary" onClick={() => { handlePick(selectedTicket.ticketId); setSelectedTicket(null); }}>Start Work</button>
                )}
                {selectedTicket?.status === 'IN_PROGRESS' && (
                  <button className="btn btn-success" onClick={() => handleResolve(selectedTicket.ticketId)}>Resolve</button>
                )}
                <button className="btn btn-secondary" onClick={() => setSelectedTicket(null)}>Close</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
