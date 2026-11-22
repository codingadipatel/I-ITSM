import axios from "axios";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 20000,
});
api.interceptors.request.use((c) => {
  const token = sessionStorage.getItem("iitsm-token");
  if (token) c.headers.Authorization = `Bearer ${token}`;
  return c;
});
api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401 && !e.config?.url?.startsWith("/auth/"))
      window.dispatchEvent(new Event("iitsm-session-expired"));
    return Promise.reject(e);
  },
);
export const errorMessage = (error: unknown): string =>
  axios.isAxiosError(error)
    ? error.response?.data?.message ||
      "Unable to reach the service. Please try again."
    : error instanceof Error
      ? error.message
      : "Something went wrong.";
export async function get<T>(path: string): Promise<T> {
  return (await api.get<T>(path)).data;
}
