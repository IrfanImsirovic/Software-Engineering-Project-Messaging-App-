import React, { useState, useEffect } from 'react';
import userIcon from "../../assets/icons/user.png";

const API_URL = import.meta.env.VITE_API_URL;

const profileCache = new Map();

export const clearProfileCache = (username) => {
  if (username) {
    profileCache.delete(username);
  }
};

const ProfilePicture = ({ username, size = 'medium', className = '' }) => {
  const [profilePicture, setProfilePicture] = useState(null);
  const [loading, setLoading] = useState(false);
  const [timestamp, setTimestamp] = useState(Date.now());

  const fetchProfilePicture = () => {
    if (!username) return;

    const cached = profileCache.get(username);
    if (cached) {
      const age = Date.now() - cached.timestamp;
      if (age < 30000) { 
        setProfilePicture(cached.url);
        return;
      }
    }

    setLoading(true);
    const token = localStorage.getItem("authToken");
    
    fetch(`${API_URL}/api/users/profile?username=${username}`, {
      headers: {
        "Authorization": "Bearer " + token
      }
    })
      .then(res => res.json())
      .then(data => {
        if (data.profilePictureUrl) {
          const fullUrl = `${API_URL}${data.profilePictureUrl}?t=${Date.now()}`; 
          setProfilePicture(fullUrl);
          profileCache.set(username, { url: fullUrl, timestamp: Date.now() }); 
        } else {
          profileCache.set(username, { url: null, timestamp: Date.now() }); 
        }
      })
      .catch(error => {
        console.error("Error fetching profile picture:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    const handleProfileUpdate = (event) => {
      if (event.detail.username === username) {
        profileCache.delete(username);
        setTimestamp(Date.now()); 
        fetchProfilePicture();
      }
    };

    window.addEventListener('profilePictureUpdated', handleProfileUpdate);
    
    return () => {
      window.removeEventListener('profilePictureUpdated', handleProfileUpdate);
    };
  }, [username]);

  useEffect(() => {
    fetchProfilePicture();
  }, [username, timestamp]);

  let sizeClass = '';
  switch (size) {
    case 'small':
      sizeClass = 'profile-picture-sm';
      break;
    case 'large':
      sizeClass = 'profile-picture-lg';
      break;
    default:
      sizeClass = 'profile-picture-md';
  }

  return (
    <div className={`profile-picture-container ${sizeClass} ${className} ${loading ? 'loading' : ''}`}>
      <img
        src={profilePicture || userIcon}
        alt={username || "User"}
        className="profile-image"
        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        onError={() => {
          setProfilePicture(null);
          if (profileCache.has(username)) {
            profileCache.delete(username);
          }
        }}
      />
    </div>
  );
};

export default ProfilePicture; 