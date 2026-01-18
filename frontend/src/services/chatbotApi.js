import axiosClient from "./axiosClient";

export async function sendMessage(sessionId, message) {
  const res = await axiosClient.post(`/chatbot/message`, { sessionId, message });
  return res.data;
}
