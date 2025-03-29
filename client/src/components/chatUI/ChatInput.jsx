import React, { useState } from 'react';

export default function ChatInput() {
  const [input, setInput] = useState('');

  const sendMessage = () => {
    if (input.trim() === '') return;
    console.log('Message sent:', input); 
    setInput('');
  };

  return (
    <div className="chat-input">
      <input
        type="text"
        placeholder="Type a message..."
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
      />
      <button onClick={sendMessage}>Send</button>
    </div>
  );
}
