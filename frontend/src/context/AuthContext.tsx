import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
} from "react";
import { api, get } from "../services/api";
import type { User } from "../types";
interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  refresh: () => Promise<void>;
}
const AuthContext = createContext<AuthState>(null!);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const logout = () => {
    sessionStorage.removeItem("iitsm-token");
    setUser(null);
  };
  const refresh = async () => {
    setUser(await get<User>("/users/me"));
  };
  useEffect(() => {
    if (sessionStorage.getItem("iitsm-token"))
      refresh()
        .catch(logout)
        .finally(() => setLoading(false));
    else setLoading(false);
    window.addEventListener("iitsm-session-expired", logout);
    return () => window.removeEventListener("iitsm-session-expired", logout);
  }, []);
  async function authenticate(path: string, data: unknown) {
    const r = (await api.post<{ token: string; user: User }>(path, data)).data;
    sessionStorage.setItem("iitsm-token", r.token);
    setUser(r.user);
  }
  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login: (email, password) =>
          authenticate("/auth/login", { email, password }),
        register: (name, email, password) =>
          authenticate("/auth/register", { name, email, password }),
        logout,
        refresh,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => useContext(AuthContext);
