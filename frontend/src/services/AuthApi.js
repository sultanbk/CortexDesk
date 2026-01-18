import axiosClient from "./axiosClient";

export async function login(credentials) {
  const res = await axiosClient.post("/auth/login", credentials);
  return res.data;
}
