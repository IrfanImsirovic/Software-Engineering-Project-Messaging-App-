import React, { useState, useEffect } from 'react';
import userIcon from "../../assets/icons/user.png";

const API_URL = import.meta.env.VITE_API_URL;

// Cache of username to profile picture URL with timestamp
const profileCache = new Map();

// Function to clear cache for a specific user
export const clearProfileCache = (username) => {
  if (username) {
    profileCache.delete(username);
  }
};

const ProfilePicture = ({ username, size = 'medium', className = '' }) => {
  const [profilePicture, setProfilePicture] = useState(null);
  const [loading, setLoading] = useState(false);
  const [timestamp, setTimestamp] = useState(Date.now());

  // Function to fetch profile picture
  const fetchProfilePicture = () => {
    if (!username) return;

    // If we have this user's profile in cache and it's not too old (less than 30 seconds), use it
    const cached = profileCache.get(username);
    if (cached) {
      const age = Date.now() - cached.timestamp;
      if (age < 30000) { // 30 seconds cache TTL
        setProfilePicture(cached.url);
        return;
      }
    }

    // Otherwise, fetch from API
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
          const fullUrl = `${API_URL}${data.profilePictureUrl}?t=${Date.now()}`; // Add timestamp to bust cache
          setProfilePicture(fullUrl);
          profileCache.set(username, { url: fullUrl, timestamp: Date.now() }); // Cache with timestamp
        } else {
          profileCache.set(username, { url: null, timestamp: Date.now() }); // Cache that user has no profile picture
        }
      })
      .catch(error => {
        console.error("Error fetching profile picture:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  // Listen for profile picture updates
  useEffect(() => {
    const handleProfileUpdate = (event) => {
      if (event.detail.username === username) {
        // Clear cache and force refresh
        profileCache.delete(username);
        setTimestamp(Date.now()); // Force re-render
        fetchProfilePicture();
      }
    };

    window.addEventListener('profilePictureUpdated', handleProfileUpdate);
    
    return () => {
      window.removeEventListener('profilePictureUpdated', handleProfileUpdate);
    };
  }, [username]);

  // Initial fetch
  useEffect(() => {
    fetchProfilePicture();
  }, [username, timestamp]);

  // Determine size class
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
          // In case of error loading the image, fallback to default
          setProfilePicture(null);
          // Remove bad URL from cache
          if (profileCache.has(username)) {
            profileCache.delete(username);
          }
        }}
      />
    </div>
  );
};

export default ProfilePicture; 