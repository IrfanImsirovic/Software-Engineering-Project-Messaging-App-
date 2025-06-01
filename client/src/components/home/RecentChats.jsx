import React, { useState, useEffect, useRef } from "react";
import "./RecentChats.css";
import userIcon from "../../assets/icons/user.png";
import groupIcon from "../../assets/icons/group_avatar.png";
import { FaUsers } from "react-icons/fa"; 
import ChatPage from "../chat-page/Chatpage"; 
import ProfilePicture from "../common/ProfilePicture";
import "../common/ProfilePicture.css";
import { Client } from "@stomp/stompjs";

const API_URL = import.meta.env.VITE_API_URL;
const SOCKET_URL = import.meta.env.VITE_API_URL.replace(/^http/, "ws") + "/ws";

export default function RecentChats({ username ,onSelectFriend}) {
  const [recentChats, setRecentChats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showGroupModal, setShowGroupModal] = useState(false);
  const [groupName, setGroupName] = useState("");
  const [selectedFriends, setSelectedFriends] = useState([]);
  const [friendsList, setFriendsList] = useState([]);
  const [selectedChat, setSelectedChat] = useState(null); 
  const [groupNameError, setGroupNameError] = useState(""); 
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);

  const toggleFriendSelection = (friend) => {
    setSelectedFriends((prevSelected) =>
      prevSelected.includes(friend)
        ? prevSelected.filter((f) => f !== friend)
        : [...prevSelected, friend]
    );
  };

  useEffect(() => {
    if (!username) return;
    
    const token = localStorage.getItem("authToken");
    if (!token) return;
    
    const client = new Client({
      brokerURL: SOCKET_URL,
      connectHeaders: { Authorization: "Bearer " + token.trim() },
      reconnectDelay: 5000,
      debug: (str) => console.log("STOMP DEBUG (RecentChats):", str),
      onConnect: () => {
        setConnected(true);
        
        const directMessageTopic = `/topic/messages/${username}`;
        
        client.subscribe(directMessageTopic, (message) => {
          const msg = JSON.parse(message.body);
          refreshRecentChats();
        });
        
        const groupMessagesTopic = `/topic/user/${username}/groups`;
        
        client.subscribe(groupMessagesTopic, (message) => {
          
          refreshRecentChats();
        });
      },
      onStompError: (frame) => {
        console.error("RecentChats: Broker error:", frame.headers["message"]);
      },
      onDisconnect: () => {
        console.log("RecentChats: Disconnected");
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [username]);

  useEffect(() => {
    if (showGroupModal) {
      const token = localStorage.getItem("authToken");
      fetch(`${API_URL}/api/friends/list?username=${username}`, {
        headers: { "Authorization": "Bearer " + token },
      })
        .then((res) => res.json())
        .then((data) => setFriendsList(data))
        .catch(console.error);
    }
  }, [showGroupModal]);

  useEffect(() => {
    if (!username) return;
    
    const pollingInterval = setInterval(() => {
      refreshRecentChats();
    }, 30000);
    
    return () => {
      clearInterval(pollingInterval);
    };
  }, [username]);

  const handleCreateGroup = () => {
    setGroupNameError("");
    
    if (!groupName.trim()) {
      setGroupNameError("Group name is required");
      return;
    }
    
    const token = localStorage.getItem("authToken");

    fetch(`${API_URL}/api/groups/create`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token,
      },
      body: JSON.stringify({
        name: groupName,
        ownerId: username,
        memberUsernames: selectedFriends, 
      }),
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to create group");
        return res.json();
      })
      .then((data) => {
        console.log("Group created:", data);
        alert("Group created!");
        setShowGroupModal(false);
        setGroupName("");
        setSelectedFriends([]);
        setGroupNameError("");

        setTimeout(() => {
          refreshRecentChats();
        }, 500);
      })
      .catch((err) => {
        console.error(err);
        alert("Error creating group");
      });
  };

  const refreshRecentChats = () => {
    const token = localStorage.getItem("authToken");
    if (!token) return;

    const requestTimestamp = Date.now();

    fetch(`${API_URL}/api/messages/recent`, {
      headers: {
        "Authorization": "Bearer " + token,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch recent chats");
        return res.json();
      })
      .then((data) => {
        const sortedChats = [...data].sort((a, b) => {
          const timeA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
          const timeB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
          return timeB - timeA;
        });
        setRecentChats(sortedChats);
      })
      .catch((err) => {
        console.error(`Error fetching recent chats (${requestTimestamp}):`, err);
        setError("Could not load recent chats");
      });
  };

  useEffect(() => {
    if (!username) return;

    const token = localStorage.getItem("authToken");
    if (!token) return;

    setLoading(true);
    setError(null);

    fetch(`${API_URL}/api/messages/recent`, {
      headers: {
        "Authorization": "Bearer " + token,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch recent chats");
        return res.json();
      })
      .then((data) => {
        setRecentChats(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Error fetching recent chats:", err);
        setError("Could not load recent chats");
        setLoading(false);
      });
  }, [username]);

  const formatTime = (timestamp) => {
    if (!timestamp) return "";

    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    
    if (isNaN(date.getTime())) return "";
    
    const now = new Date();

    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }

    const daysDiff = Math.floor((now - date) / (1000 * 60 * 60 * 24));
    if (daysDiff < 7) {
      return date.toLocaleDateString([], { weekday: "short" });
    }

    return date.toLocaleDateString([], { month: "short", day: "numeric" });
  };

  const getChatPartner = (chat) => {
    if (chat.isGroup) {
      return chat.name; 
    }

    if (chat.sender === username) {
      return chat.receiver;
    }
    return chat.sender;
  };

  const truncateMessage = (message, maxLength = 40) => {
    if (!message) return "";
    if (message.length <= maxLength) return message;
    return message.substring(0, maxLength) + "...";
  };

  const handleSelectChat = (chat) => {
    let formattedChat;
    
    if (chat.isGroup) {
      formattedChat = {
        id: chat.id,
        name: chat.name,
        isGroup: true
      };
    } else if (chat.sender && chat.receiver) {
      formattedChat = chat.sender === username ? chat.receiver : chat.sender;
    } else {

      formattedChat = chat;
    }
    
    onSelectFriend(formattedChat);
  };

  const renderRecentChat = (chat, index) => {
    const isGroup = chat.isGroup === true;
    const chatName = isGroup ? chat.name : (chat.sender === username ? chat.receiver : chat.sender);
    const content = chat.content || "";
    const showSenderName = chat.sender === username && !isGroup;
    
    const hasImage = (chat.imageUrl && chat.imageUrl.trim() !== '') || 
                     (chat.image_url && chat.image_url.trim() !== '') || 
                     (chat.image && chat.image.trim() !== '') ||
                     (content === "" && chat.id); 
                    
    const senderName = chat.sender === username ? "You" : chat.sender;
    
    let timestamp = chat.timestamp;
    if (timestamp && typeof timestamp === 'string') {
      timestamp = new Date(timestamp);
    }

    return (
      <li key={`${isGroup ? 'group' : 'direct'}-${index}`} className="chat-item" onClick={() => handleSelectChat(chat)}>
        <div className="avatar">
          {isGroup ? 
            <img src={groupIcon} alt="Group" /> : 
            <ProfilePicture 
              username={chat.sender === username ? chat.receiver : chat.sender} 
              size="medium"
              key={`chat-${index}-${Date.now()}`}
            />
          }
        </div>

        <div className="chat-details">
          <div className="chat-content">
            <span className="username">{chatName}</span>
            <div className="message-preview">
              {hasImage ? (
                <span className="message-text">
                  {chat.sender === username ? (
                    <span><span className="you">You</span> sent an image</span>
                  ) : (
                    <span>{chat.sender} sent an image</span>
                  )}
                  {content && ` - ${truncateMessage(content)}`}
                </span>
              ) : showSenderName ? (
                <span className="message-text">
                  <span className="you">You: </span>
                  {truncateMessage(content)}
                </span>
              ) : (
                <span className="message-text">
                  {chat.sender !== "SYSTEM" && chat.sender !== username && !isGroup && `${chat.sender}: `}
                  {content ? truncateMessage(content) : "Sent a message"}
                </span>
              )}
            </div>
          </div>
          <span className="timestamp">{formatTime(timestamp)}</span>
        </div>
      </li>
    );
  };

  return (
    <div className="recent-chats">

      <div className="create-group-wrapper">
        <button className="create-group-button" onClick={() => setShowGroupModal(true)}>
          <FaUsers className="icon" />
          <span>Create Group Chat</span>
        </button>
      </div>

      <h2>Recent Chats</h2>
      {loading && <div className="loading">Loading your chats...</div>}
      {!loading && error && <div className="error-message">{error}</div>}
      {!loading && !error && recentChats.length === 0 && (
        <div className="no-chats">No recent conversations</div>
      )}

      <ul className="chat-list">
        {recentChats.map((chat, index) => renderRecentChat(chat, index))}
      </ul>
     
      {selectedChat && (
        <div className="chat-display">
          <ChatPage username={username} chat={selectedChat} />
        </div>
      )}

      {showGroupModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Create Group Chat</h2>
            <input
              type="text"
              placeholder="Enter group name"
              className="modal-input"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
            />
            {groupNameError && <p className="error-message">{groupNameError}</p>}

            <div className="friend-list">
              {friendsList.map((friend) => (
                <label key={friend}>
                  <input
                    type="checkbox"
                    checked={selectedFriends.includes(friend)}
                    onChange={() => toggleFriendSelection(friend)}
                  />
                  <span className="username">{friend}</span>
                </label>
              ))}
            </div>

            <div className="modal-buttons">
              <button className="create-btn" onClick={handleCreateGroup}>
                Create
              </button>
              <button className="cancel-btn" onClick={() => {
                setShowGroupModal(false);
                setGroupNameError(""); 
              }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
