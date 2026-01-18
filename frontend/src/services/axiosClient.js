import axios from "axios";
import { getCurrentUser, logout } from "../auth/auth";

const axiosClient = axios.create({
  baseURL: "http://localhost:9091/api",
  headers: { "Content-Type": "application/json" },
});

axiosClient.interceptors.request.use((config) => {
  const user = getCurrentUser();
  if (user?.token) config.headers.Authorization = `Bearer ${user.token}`;
  return config;
});

axiosClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      try {
        logout();
      } catch (e) {
        // ignore
      }
      // redirect to root/login
      window.location.href = "/";
    }
    return Promise.reject(err);
  }
);

export default axiosClient;
