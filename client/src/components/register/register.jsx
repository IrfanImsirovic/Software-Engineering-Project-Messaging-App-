import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./register.module.css";
import eyeIcon from "../../assets/icons/eye.png";
import hiddenIcon from "../../assets/icons/hidden.png";

export default function Register() {
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
  });

  const [errorMessage, setErrorMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate(); 

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");

    try {
      const response = await fetch(import.meta.env.VITE_API_URL + "/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      const isJson = response.headers.get("content-type")?.includes("application/json");
      const data = isJson ? await response.json() : { error: await response.text() };

      if (!response.ok) {
        throw new Error(data.error || "Registration failed");
      }

      navigate("/login"); 

    } catch (error) {
      console.error("❌ Registration error:", error);
      setErrorMessage(error.message);
    }
  };

  const isFormValid = formData.username && formData.email && formData.password;

  return (
    <div className="register-page">
      <div className={styles.formContainer}>
        <h2 className={styles.title}>Register</h2>
        {errorMessage && <p className="error-message">{errorMessage}</p>}
        <form onSubmit={handleSubmit}>
          <div className={styles.formField}>
            <label htmlFor="username" className={styles.label}>Username</label>
            <input
              className={styles.input}
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Enter your username"
              required
            />
          </div>
          <div className={styles.formField}>
            <label htmlFor="email" className={styles.label}>Email</label>
            <input
              className={styles.input}
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Enter your email"
              required
            />
          </div>
          <div className={styles.formField}>
            <label htmlFor="password" className={styles.label}>Password</label>
            <input
              type={showPassword ? "text" : "password"}
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Enter your password"
              required
              className={`${styles.input} ${styles.passwordInput}`}
            />
            <span
              onClick={() => setShowPassword((prev) => !prev)}
              className={styles.passwordToggleIcon}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              tabIndex={0}
              onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') setShowPassword((prev) => !prev); }}
            >
              <img
                src={showPassword ? hiddenIcon : eyeIcon}
                alt={showPassword ? 'Hide password' : 'Show password'}
                className={styles.passwordToggleImg}
              />
            </span>
          </div>
          <button type="submit" className={styles.registerButton} disabled={!isFormValid} style={{ opacity: isFormValid ? 1 : 0.5 }}>
            Register
          </button>
        </form>
        <p className={styles.loginLink}>
          Already have an account?{" "}
          <span onClick={() => navigate("/login")}>Login</span>
        </p>
      </div>
    </div>
  );
}
