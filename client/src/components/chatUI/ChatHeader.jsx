export default function ChatHeader() {
  return (
    <div className="chat-header">
      <div className="chat-header-left">
        <button className="back-button">Back</button>
        <img
          src="https://img.freepik.com/premium-vector/avatar-profile-icon-flat-style-male-user-profile-vector-illustration-isolated-background-man-profile-sign-business-concept_157943-38764.jpg?semt=ais_hybrid"
          alt="Profile"
          className="chat-avatar"
        />
      </div>

      <div className="chat-header-center">
        <span className="chat-username">Ahmed</span>
      </div>
    </div>
  );
}
