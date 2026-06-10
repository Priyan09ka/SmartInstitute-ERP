import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiRequest } from "../services/Api";

function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState({ type: "", text: "" });

  const sendCode = async () => {
    if (!email.trim()) {
      setMsg({ type: "err", text: "Enter your email first." });
      return;
    }
    setLoading(true);
    setMsg({ type: "", text: "" });
    try {
      const data = await apiRequest("/auth/forgot-password", "POST", { email });
      setMsg({ type: "ok", text: data?.message || "If your email exists, reset code has been sent." });
      setStep(2);
    } catch (err) {
      setMsg({ type: "err", text: err?.message || "Could not send reset code." });
    } finally {
      setLoading(false);
    }
  };

  const resetPassword = async () => {
    if (!email.trim() || !code.trim() || !newPassword) {
      setMsg({ type: "err", text: "Email, code, and new password are required." });
      return;
    }
    if (newPassword.length < 6) {
      setMsg({ type: "err", text: "Password must be at least 6 characters." });
      return;
    }
    if (newPassword !== confirmPassword) {
      setMsg({ type: "err", text: "Passwords do not match." });
      return;
    }
    setLoading(true);
    setMsg({ type: "", text: "" });
    try {
      const data = await apiRequest("/auth/reset-password", "POST", {
        email,
        code,
        newPassword,
      });
      setMsg({ type: "ok", text: data?.message || "Password reset successful." });
      setTimeout(() => navigate("/"), 900);
    } catch (err) {
      setMsg({ type: "err", text: err?.message || "Could not reset password." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-4">
      <div className="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Forgot password</h1>
        <p className="text-gray-500 mb-6 text-sm">
          Enter your email to receive a reset code, then set a new password.
        </p>

        <label className="block text-sm text-gray-700 mb-1">Email</label>
        <input
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          type="email"
          placeholder="Enter email"
          className="w-full p-3 border border-gray-300 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-indigo-400"
        />

        {step === 2 && (
          <>
            <label className="block text-sm text-gray-700 mb-1">Reset code</label>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              type="text"
              placeholder="Enter 6-digit code"
              className="w-full p-3 border border-gray-300 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-indigo-400"
            />

            <label className="block text-sm text-gray-700 mb-1">New password</label>
            <input
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              type="password"
              placeholder="Minimum 6 characters"
              className="w-full p-3 border border-gray-300 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-indigo-400"
            />

            <label className="block text-sm text-gray-700 mb-1">Confirm password</label>
            <input
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              type="password"
              placeholder="Re-enter new password"
              className="w-full p-3 border border-gray-300 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-indigo-400"
            />
          </>
        )}

        {msg.text && (
          <p className={`text-sm mb-4 ${msg.type === "ok" ? "text-emerald-700" : "text-red-700"}`}>{msg.text}</p>
        )}

        {step === 1 ? (
          <button
            type="button"
            onClick={sendCode}
            disabled={loading}
            className="w-full bg-indigo-600 text-white p-3 rounded-lg font-semibold hover:bg-indigo-700 transition duration-300 disabled:opacity-60"
          >
            {loading ? "Sending..." : "Send reset code"}
          </button>
        ) : (
          <button
            type="button"
            onClick={resetPassword}
            disabled={loading}
            className="w-full bg-indigo-600 text-white p-3 rounded-lg font-semibold hover:bg-indigo-700 transition duration-300 disabled:opacity-60"
          >
            {loading ? "Updating..." : "Reset password"}
          </button>
        )}

        <p className="text-sm text-gray-600 mt-4">
          <Link to="/" className="text-indigo-600 hover:underline">
            Back to login
          </Link>
        </p>
      </div>
    </div>
  );
}

export default ForgotPassword;
