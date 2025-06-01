import React, { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import "./Chatpage.css";
import sendIcon from "../../assets/icons/send_icon.png";
import galleryIcon from "../../assets/icons/gallery.png";
import userIcon from "../../assets/icons/user.png";
import ProfilePicture from "../common/ProfilePicture";
import "../common/ProfilePicture.css";

const API_URL = import.meta.env.VITE_API_URL;
const SOCKET_URL = import.meta.env.VITE_API_URL.replace(/^http/, "ws") + "/ws";

export default function ChatPage({ username, chat }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [imageLoading, setImageLoading] = useState({});
  const [modalImage, setModalImage] = useState(null);
  const clientRef = useRef(null);
  const chatBoxRef = useRef(null);
  const fileInputRef = useRef(null);

  const isGroup = chat?.isGroup === true;
  const chatTitle = isGroup ? chat.name : (chat.username || chat);

  // Auto-scroll
  useEffect(() => {
    chatBoxRef.current?.scrollTo(0, chatBoxRef.current.scrollHeight);
  }, [messages]);

  // Load history
  useEffect(() => {
    const token = localStorage.getItem("authToken");
    const url = isGroup
      ? `${API_URL}/api/group-messages/history?groupId=${chat.id}`
      : `${API_URL}/api/messages/history?friendUsername=${chat.username || chat}`;

    fetch(url, {
      headers: { Authorization: "Bearer " + token },
    })
      .then((res) => res.json())
      .then(setMessages)
      .catch(console.error);
  }, [chat, username]);

  // WebSocket connection
  useEffect(() => {
    const token = localStorage.getItem("authToken");
    const client = new Client({
      brokerURL: SOCKET_URL,
      connectHeaders: { Authorization: "Bearer " + token.trim() },
      reconnectDelay: 5000,
      debug: (str) => console.log("STOMP DEBUG:", str),
      onConnect: () => {
        console.log("✅ Connected to WebSocket");
        setConnected(true);
        if (isGroup) {
          const topic = `/topic/group/${chat.id}`;
          console.log("📡 Subscribing to group topic:", topic);
          client.subscribe(topic, (message) => {
            const receivedMsg = JSON.parse(message.body);
            console.log("📨 Received group message:", receivedMsg);
            console.log("📨 Image URL in message:", receivedMsg.imageUrl);
            
            // Map the received message to the format expected by the UI
            const formattedMsg = {
              groupId: chat.id,
              sender: receivedMsg.sender,
              content: receivedMsg.content,
              timestamp: receivedMsg.timestamp,
              imageUrl: receivedMsg.imageUrl,
              id: receivedMsg.id // Make sure we have an ID for image loading state
            };
            
            setMessages((prev) => [...prev, formattedMsg]);
          });
        } else {
          const topic = `/topic/messages/${username}`;
          console.log("📡 Subscribing to topic:", topic);

          client.subscribe(topic, (message) => {
            const msg = JSON.parse(message.body);
            console.log("📨 Received message:", msg);
            console.log("📨 Image URL in direct message:", msg.imageUrl);
            
            setMessages((prev) => [...prev, msg]);
          });
        }
      },
      onStompError: (frame) => {
        console.error("❌ Broker error:", frame.headers["message"]);
      },
      onDisconnect: () => {
        console.log("🔌 Disconnected");
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [username, chat]);

  // Handle image upload
  const handleImageUpload = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file is an image
    if (!file.type.startsWith('image/')) {
      alert('Please select an image file');
      return;
    }

    setUploading(true);

    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem("authToken");
    fetch(`${API_URL}/api/uploads/image`, {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token
      },
      body: formData
    })
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.json();
      })
      .then(data => {
        // Send message with image
        console.log("📸 Upload response:", data);
        sendMessageWithImage(data.url);
      })
      .catch(error => {
        console.error('Error uploading image:', error);
        alert('Failed to upload image');
      })
      .finally(() => {
        setUploading(false);
        // Clear the file input
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      });
  };

  // Send message with image
  const sendMessageWithImage = (imageUrl) => {
    const client = clientRef.current;
    if (!connected || !client?.connected) return;

    const now = new Date().toISOString();
    const content = input.trim() || ""; // Allow empty content for image-only messages

    const messagePayload = isGroup
      ? {
          groupId: chat.id,
          sender: username,
          content: content,
          timestamp: now,
          imageUrl: imageUrl
        }
      : {
          sender: username,
          receiver: chat.username || chat,
          content: content,
          timestamp: now,
          imageUrl: imageUrl
        };

    const token = localStorage.getItem("authToken");
    client.publish({
      destination: isGroup ? "/app/group" : "/app/chat",
      body: JSON.stringify(messagePayload),
      headers: { 
        'Authorization': 'Bearer ' + token.trim() 
      }
    });

    setInput("");
  };

  // Send text message
  const sendMessage = () => {
    const client = clientRef.current;
    if (!input.trim() || !connected || !client?.connected) return;

    const now = new Date().toISOString();

    const messagePayload = isGroup
      ? {
          groupId: chat.id,
          sender: username,
          content: input.trim(),
          timestamp: now,
        }
      : {
          sender: username,
          receiver: chat.username || chat,
          content: input.trim(),
          timestamp: now,
        };

    const token = localStorage.getItem("authToken");
    client.publish({
      destination: isGroup ? "/app/group" : "/app/chat",
      body: JSON.stringify(messagePayload),
      headers: { 
        'Authorization': 'Bearer ' + token.trim() 
      }
    });

    // Don't add message locally - let the WebSocket handle it
    // The message will be received back through the subscription
    setInput("");
  };

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  const isFirstInSequence = (index) =>
    index === 0 || messages[index].sender !== messages[index - 1].sender;

  // Open image in modal
  const openImageModal = (imageUrl) => {
    setModalImage(imageUrl);
  };

  // Close image modal
  const closeImageModal = () => {
    setModalImage(null);
  };

  return (
    <div className="chat-container">
      <div className="chat-header">
        <div className="friend-profile">
          <div className="friend-avatar">
            <ProfilePicture username={chatTitle} />
          </div>
          <span className="friend-name">{chatTitle}</span>
        </div>
        {!connected && <p className="connection-status">🔌 Connecting...</p>}
      </div>

      <div className="chat-box" ref={chatBoxRef}>
        {messages.map((msg, idx) => {
          console.log("🧾 Rendering message:", msg)
          const isMe = msg.sender === username;
          const isSystem = msg.sender === "SYSTEM";
          const first = isFirstInSequence(idx);
          const hasImage = msg.imageUrl && msg.imageUrl.trim() !== '';

          // Handle system messages differently
          if (isSystem) {
            return (
              <div key={idx} className="system-wrapper">
                <div className="message-bubble system">
                  {msg.content}
                </div>
              </div>
            );
          }

          return (
            <div
              key={idx}
              className={`message-wrapper ${isMe ? "sent-wrapper" : "received-wrapper"}`}
            >
              {!isMe && first && (
                <div className="sender-avatar">
                  <ProfilePicture username={msg.sender} />
                </div>
              )}
              {!isMe && !first && <div className="avatar-spacer"></div>}

              <div className={`message-container ${isMe ? "sent-container" : "received-container"}`}>
                <div className={`message-bubble ${isMe ? "sent" : "received"} ${hasImage && !msg.content ? "image-only" : ""}`}>
                  {!isMe && first && <div className="message-sender">{msg.sender}</div>}
                  <div className="message-content-wrapper">
                    {msg.content && <div className="message-content">{msg.content}</div>}
                    {hasImage && (
                      <div className={`message-image-container ${!msg.content ? "image-only-container" : ""}`}>
                        {imageLoading[msg.id] !== false && <div className="image-loading-indicator"></div>}
                        <img 
                          src={`${API_URL}${msg.imageUrl}?t=${msg.timestamp || Date.now()}`} 
                          alt="Shared image" 
                          className={`message-image ${imageLoading[msg.id] === false ? 'loaded' : ''}`}
                          onClick={() => openImageModal(`${API_URL}${msg.imageUrl}?t=${msg.timestamp || Date.now()}`)}
                          onLoad={() => {
                            console.log("✅ Image loaded successfully:", `${API_URL}${msg.imageUrl}`);
                            setImageLoading(prev => ({...prev, [msg.id]: false}));
                            chatBoxRef.current?.scrollTo(0, chatBoxRef.current.scrollHeight);
                          }}
                          onError={(e) => {
                            console.error("❌ Failed to load image:", `${API_URL}${msg.imageUrl}`, e);
                            setImageLoading(prev => ({...prev, [msg.id]: false}));
                          }}
                        />
                      </div>
                    )}
                    <span className="message-time">{formatTime(msg.timestamp)}</span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Image Modal */}
      {modalImage && (
        <div className="image-modal-overlay" onClick={closeImageModal}>
          <div className="image-modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close-button" onClick={closeImageModal}>&times;</button>
            <img src={modalImage} alt="Enlarged view" />
          </div>
        </div>
      )}

      <div className="chat-footer">
        <div className="chat-input">
          <input
            type="text"
            placeholder="Type a message..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage()}
            disabled={!connected || uploading}
          />
          
          {/* Hidden file input */}
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            accept="image/*"
            onChange={handleImageUpload}
            disabled={!connected || uploading}
          />
          
          {/* Gallery button */}
          <button 
            onClick={() => fileInputRef.current && fileInputRef.current.click()} 
            disabled={!connected || uploading}
            className="gallery-button"
          >
            <img src={galleryIcon} alt="Gallery" />
          </button>
          
          {/* Send button */}
          <button 
            onClick={sendMessage} 
            disabled={!connected || uploading || !input.trim()}
          >
            <img src={sendIcon} alt="Send" />
          </button>
        </div>
      </div>
    </div>
  );
}
