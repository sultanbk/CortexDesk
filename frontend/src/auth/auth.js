export function saveUserSession(data) {
  localStorage.setItem(
    "user",
    JSON.stringify({
      token: data.token,
      role: data.role,
      username: data.username,
      userId: data.userId,
    })
  );
}

export function getCurrentUser() {
  const user = localStorage.getItem("user");
  return user ? JSON.parse(user) : null;
}

export function logout() {
  localStorage.removeItem("user");
}
