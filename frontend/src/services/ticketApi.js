import axiosClient from "./axiosClient";

export async function createTicket(data) {
  const res = await axiosClient.post("/tickets", data);
  return res.data;
}

export async function getAllTickets() {
  const res = await axiosClient.get("/tickets/all");
  return res.data;
}

export async function getIssueCategories() {
  const res = await axiosClient.get("/issue-categories");
  return res.data;
}

export async function setPriority(ticketId, priority) {
  const res = await axiosClient.put(`/tickets/${ticketId}/priority`, { priority });
  return res.data;
}

export async function assignEngineer(ticketId, engineerId) {
  const res = await axiosClient.put(`/tickets/${ticketId}/assign`, { engineerId });
  return res.data;
}

export async function resolveTicket(data) {
  const res = await axiosClient.post(`/tickets/resolve`, data);
  return res.data;
}

export async function closeTicket(data) {
  const res = await axiosClient.post(`/tickets/close`, data);
  return res.data;
}

export async function assignTicket(data) {
  const res = await axiosClient.post(`/tickets/assign`, data);
  return res.data;
}

export async function reopenTicket(data) {
  const res = await axiosClient.post(`/tickets/reopen`, data);
  return res.data;
}

export async function pickTicket(data) {
  const res = await axiosClient.post(`/tickets/pick`, data);
  return res.data;
}

export async function autoAssignTicket(ticketId) {
  const res = await axiosClient.post(`/tickets/${ticketId}/autoassign`);
  return res.data;
}

export async function getEngineerQueue(engineerId) {
  const res = await axiosClient.get(`/tickets/engineer/${engineerId}`);
  return res.data;
}

export async function uploadAttachment(ticketId, file) {
  const fd = new FormData();
  fd.append('file', file);
  const res = await axiosClient.post(`/tickets/${ticketId}/attachments`, fd, { headers: { 'Content-Type': 'multipart/form-data' } });
  return res.data;
}

export async function listAttachments(ticketId) {
  const res = await axiosClient.get(`/tickets/${ticketId}/attachments`);
  return res.data;
}

export async function downloadAttachment(id) {
  const res = await axiosClient.get(`/tickets/attachments/${id}/download`, { responseType: 'blob' });
  return res.data;
}

export async function getTicketHistory(ticketId) {
  const res = await axiosClient.get(`/tickets/${ticketId}/history`);
  return res.data;
}
