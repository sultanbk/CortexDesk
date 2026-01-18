import axiosClient from "./axiosClient";

export const getIssueCategories = async () => {
  const res = await axiosClient.get('/api/issue-categories');
  return res.data;
};

export const createIssueCategory = async (payload) => {
  const res = await axiosClient.post('/api/issue-categories', payload);
  return res.data;
};

export const updateIssueCategory = async (id, payload) => {
  const res = await axiosClient.put(`/api/issue-categories/${id}`, payload);
  return res.data;
};

export const deleteIssueCategory = async (id) => {
  const res = await axiosClient.delete(`/api/issue-categories/${id}`);
  return res.data;
};
