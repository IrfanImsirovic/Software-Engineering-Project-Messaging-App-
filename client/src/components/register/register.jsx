import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./register.css";
import ImageUpload from "../UIElements/ImageUpload";

export default function Register() {
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
  });
  const [imageFile, setImageFile] = useState(null);


  const [errorMessage, setErrorMessage] = useState("");
  const navigate = useNavigate(); // Hook for redirection

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");
  
    const formPayload = new FormData();
    formPayload.append("username", formData.username);
    formPayload.append("email", formData.email);
    formPayload.append("password", formData.password);
    if (imageFile) {
      formPayload.append("image", imageFile);
    }
  
    try {
      const response = await fetch(import.meta.env.VITE_API_URL + "/api/auth/register", {
        method: "POST",
        body: formPayload,
      });
  
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || "Registration failed");
      }
  
      console.log("✅ Registration successful:", data);
      navigate("/login");
    } catch (error) {
      console.error("❌ Registration error:", error);
      setErrorMessage(error.message);
    }
  };
  

  return (
    <div className="register-page">
      <div className="form-container">
        <h2>Register</h2>
        {errorMessage && <p className="error-message">{errorMessage}</p>}
        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Enter your username"
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Enter your email"
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Enter your password"
              required
            />
            <ImageUpload
              center
              id="image"
              onInput={(id, file, isValid) => {
                if (isValid) {
                  setImageFile(file);
                }
              }}
              errorText="Please select an image"
            />

          </div>
          <button type="submit" className="register-button">Register</button>
        </form>
        <p className="login-link">
          Already have an account?{" "}
          <span onClick={() => navigate("/login")}>Login</span>
        </p>
      </div>
    </div>
  );
}