import React, { useState, useRef, useEffect } from "react";
import "./UserProfile.css";
import userIcon from "../../assets/icons/user.png";

const API_URL = import.meta.env.VITE_API_URL;

const UserProfile = ({ username }) => {
  const [profilePicture, setProfilePicture] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [message, setMessage] = useState(null);
  const fileInputRef = useRef(null);

  // Fetch user profile on component mount
  useEffect(() => {
    const token = localStorage.getItem("authToken");
    if (!token || !username) return;

    fetch(`${API_URL}/api/users/profile?username=${username}`, {
      headers: {
        "Authorization": "Bearer " + token
      }
    })
      .then(res => res.json())
      .then(data => {
        if (data.profilePictureUrl) {
          setProfilePicture(`${API_URL}${data.profilePictureUrl}`);
        }
      })
      .catch(error => console.error("Error fetching profile:", error));
  }, [username]);

  const handlePictureClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file is an image
    if (!file.type.startsWith('image/')) {
      setMessage({ type: "error", text: "Please select an image file" });
      return;
    }

    setIsUploading(true);
    setMessage(null);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('username', username);

    const token = localStorage.getItem("authToken");
    fetch(`${API_URL}/api/users/profile-picture`, {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token
      },
      body: formData
    })
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to upload profile picture');
        }
        return response.json();
      })
      .then(data => {
        setProfilePicture(`${API_URL}${data.url}?t=${Date.now()}`);
        setMessage({ type: "success", text: "Profile picture updated successfully" });
        
        // Clear the profile cache in ProfilePicture component to force a refresh
        window.dispatchEvent(new CustomEvent('profilePictureUpdated', { 
          detail: { username, timestamp: Date.now() } 
        }));
      })
      .catch(error => {
        console.error('Error uploading profile picture:', error);
        setMessage({ type: "error", text: "Failed to upload profile picture" });
      })
      .finally(() => {
        setIsUploading(false);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      });
  };

  return (
    <div className="user-profile-container">
      <div className="user-edit-picture-container" onClick={handlePictureClick}>
        <img 
          src={profilePicture || userIcon} 
          alt="Profile" 
          className={`user-edit-picture ${isUploading ? 'uploading' : ''}`}
        />
        <div className="user-edit-picture-overlay">
          <span>Change Picture</span>
        </div>
        <input
          type="file"
          ref={fileInputRef}
          style={{ display: 'none' }}
          accept="image/*"
          onChange={handleFileChange}
        />
      </div>
      
      <h3 className="profile-username">{username}</h3>
      
      {message && (
        <div className={`profile-message ${message.type}`}>
          {message.text}
        </div>
      )}
    </div>
  );
};

export default UserProfile; 