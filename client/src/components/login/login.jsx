import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./login.module.css";
import eyeIcon from "../../assets/icons/eye.png";
import hiddenIcon from "../../assets/icons/hidden.png";

export default function Login() {
  const [credentials, setCredentials] = useState({ username: "", password: "" });
  const [errorMessage, setErrorMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setCredentials({ ...credentials, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");

    try {
      const response = await fetch(import.meta.env.VITE_API_URL + "/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: credentials.username, 
          password: credentials.password
        }),
        credentials: "include"
      });

      const isJson = response.headers.get("content-type")?.includes("application/json");
      const data = isJson ? await response.json() : { error: await response.text() };

      if (!response.ok) {
        throw new Error(data.error || "Login failed");
      }

      localStorage.setItem("authToken", data.token);
      localStorage.setItem("username", credentials.username);
      navigate("/home");

    } catch (error) {
      console.error("Login error:", error);
      setErrorMessage(error.message);
    }
  };

  const isFormValid = credentials.username && credentials.password;

  return (
    <div className="login-page">
      <div className={styles.formContainer}>
        <h2 className={styles.title}>Login</h2>
        {errorMessage && <p className="error-message">{errorMessage}</p>}
        <form onSubmit={handleSubmit}>
        <div className={styles.formField}>
          <label htmlFor="username" className={styles.label}>Username</label>
          <input
            type="text"
            id="username"
            name="username"
            value={credentials.username}
            onChange={handleChange}
            placeholder="Enter your username"
            required
            className={styles.input}
          />
        </div>
        <div className={styles.formField} style={{ position: 'relative' }}>
          <label htmlFor="password" className={styles.label}>Password</label>
          <input
            type={showPassword ? "text" : "password"}
            id="password"
            name="password"
            value={credentials.password}
            onChange={handleChange}
            placeholder="Enter your password"
            required
            className={styles.input + ' ' + styles.passwordInput}
          />
          <span
            onClick={() => setShowPassword((prev) => !prev)}
            className={styles.passwordToggleIcon}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            tabIndex={0}
            onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') setShowPassword((prev) => !prev); }}
            style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', cursor: 'pointer' }}
          >
            <img
              src={showPassword ? hiddenIcon : eyeIcon}
              alt={showPassword ? 'Hide password' : 'Show password'}
              className={styles.passwordToggleImg}
            />
          </span>
        </div>
        <button type="submit" className={styles.loginButton} disabled={!isFormValid} style={{ opacity: isFormValid ? 1 : 0.5 }}>Login</button>
        </form>
        <p className={styles.registerLink}>
          Don't have an account?{" "}
          <span onClick={() => navigate("/register")}>Register</span>
        </p>
      </div>
    </div>
  );
}
