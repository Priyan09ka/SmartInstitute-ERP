import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiRequest } from "../services/Api";

const DASHBOARD_PATH_BY_ROLE = {
  SUPER_ADMIN: "/super-admin",
  INSTITUTE_ADMIN: "/institute-admin",
  PRINCIPAL: "/principal",
  TEACHER: "/teacher",
  STUDENT: "/student",
};

function titleFromRole(role) {
  if (!role) return "User";
  return role
    .toLowerCase()
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

function Profile() {
  const navigate = useNavigate();
  const role = localStorage.getItem("role");
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editableName, setEditableName] = useState("");
  const [isEditingName, setIsEditingName] = useState(false);
  const [savingName, setSavingName] = useState(false);
  const [saveMsg, setSaveMsg] = useState({ type: "", text: "" });
  const [profileImage, setProfileImage] = useState("");
  const [photoMsg, setPhotoMsg] = useState({ type: "", text: "" });

  const dashboardPath = DASHBOARD_PATH_BY_ROLE[role] || "/dashboard";
  const isStudent = role === "STUDENT";

  useEffect(() => {
    let cancelled = false;

    const loadProfile = async () => {
      setLoading(true);
      setError("");
      try {
        let response = null;
        switch (role) {
          case "TEACHER":
            response = await apiRequest("/api/teachers/me");
            break;
          case "STUDENT":
            response = await apiRequest("/api/students/me");
            break;
          case "INSTITUTE_ADMIN":
          case "PRINCIPAL":
            response = await apiRequest("/platform/institute-admin/my-institute");
            break;
          default:
            response = null;
        }
        if (!cancelled) {
          setProfile(response);
          setEditableName(response?.name || "");
        }
      } catch (err) {
        if (!cancelled) setError(err?.message || "Could not load profile details.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadProfile();

    return () => {
      cancelled = true;
    };
  }, [role]);

  useEffect(() => {
    const imageStorageKey = profile?.email ? `profile-photo:${profile.email.toLowerCase()}` : "";
    if (!imageStorageKey) return;
    const saved = localStorage.getItem(imageStorageKey);
    setProfileImage(saved || "");
  }, [profile?.email]);

  const saveStudentName = async () => {
    const next = editableName.trim();
    if (!next) {
      setSaveMsg({ type: "err", text: "Name is required." });
      return;
    }
    setSavingName(true);
    setSaveMsg({ type: "", text: "" });
    try {
      const updated = await apiRequest("/api/students/me/name", "PATCH", { name: next });
      setProfile(updated || profile);
      setEditableName((updated?.name || next));
      setSaveMsg({ type: "ok", text: "Name updated." });
      setIsEditingName(false);
    } catch (err) {
      setSaveMsg({ type: "err", text: err?.message || "Could not update name." });
    } finally {
      setSavingName(false);
    }
  };

  const profileRows = useMemo(() => {
    const rows = [{ label: "Role", value: titleFromRole(role) }];

    if (!profile || typeof profile !== "object") return rows;

    if (profile.name) rows.push({ label: "Name", value: profile.name });
    if (profile.email) rows.push({ label: "Email", value: profile.email });
    if (isStudent && profile.rollNumber) rows.push({ label: "Roll Number", value: profile.rollNumber });
    if (profile.classroomName) rows.push({ label: "Class", value: profile.classroomName });
    if (profile.instituteName) rows.push({ label: "Institute", value: profile.instituteName });
    if (!profile.instituteName && profile.name && (role === "INSTITUTE_ADMIN" || role === "PRINCIPAL")) {
      rows.push({ label: "Institute", value: profile.name });
    }

    return rows;
  }, [profile, role, isStudent]);

  const onPhotoSelect = (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    setPhotoMsg({ type: "", text: "" });
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setPhotoMsg({ type: "err", text: "Only image files are allowed." });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setPhotoMsg({ type: "err", text: "Image size must be 5MB or less." });
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = String(reader.result || "");
      const img = new Image();
      img.onload = () => {
        const w = img.width || 0;
        const h = img.height || 0;
        if (w < 200 || h < 260) {
          setPhotoMsg({ type: "err", text: "Use a clearer photo (minimum 200x260)." });
          return;
        }
        if (h <= w) {
          setPhotoMsg({ type: "err", text: "Use a portrait face/passport photo (height should be greater than width)." });
          return;
        }
        const ratio = w / h;
        if (ratio < 0.55 || ratio > 0.85) {
          setPhotoMsg({ type: "err", text: "Use passport-style portrait ratio (about 3:4)." });
          return;
        }
        setProfileImage(dataUrl);
        if (profile?.email) {
          localStorage.setItem(`profile-photo:${profile.email.toLowerCase()}`, dataUrl);
        }
        setPhotoMsg({ type: "ok", text: "Profile photo updated." });
      };
      img.onerror = () => setPhotoMsg({ type: "err", text: "Invalid image file." });
      img.src = dataUrl;
    };
    reader.onerror = () => setPhotoMsg({ type: "err", text: "Could not read image." });
    reader.readAsDataURL(file);
  };

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-6">
      <div className="w-full max-w-2xl bg-white shadow-lg rounded-2xl p-6 sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">My profile</h1>
        <p className="text-slate-600 mt-2">Review your account details and continue to home page.</p>

        <div className="mt-6 border border-slate-200 rounded-xl p-4 bg-slate-50">
          <div className="flex flex-wrap items-center gap-4">
            <div className="w-24 h-28 rounded-lg overflow-hidden border border-slate-300 bg-white flex items-center justify-center">
              {profileImage ? (
                <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
              ) : (
                <span className="text-[11px] text-slate-400 text-center px-1">No photo</span>
              )}
            </div>
            <div className="flex-1 min-w-[220px]">
              <p className="text-sm font-medium text-slate-700">Profile photo</p>
              <p className="text-xs text-slate-500 mt-1">
                Only face/passport-style photo allowed. Use portrait image (about 3:4), up to 5MB.
              </p>
              <label className="inline-flex mt-3 cursor-pointer bg-indigo-600 text-white px-3 py-2 rounded-lg text-sm hover:bg-indigo-700">
                Upload photo
                <input type="file" accept="image/*" className="hidden" onChange={onPhotoSelect} />
              </label>
              {photoMsg.text && (
                <p className={`mt-2 text-sm ${photoMsg.type === "ok" ? "text-emerald-700" : "text-red-700"}`}>
                  {photoMsg.text}
                </p>
              )}
            </div>
          </div>
        </div>

        {loading ? (
          <p className="mt-6 text-slate-600">Loading profile details...</p>
        ) : (
          <>
            {isStudent && (
              <div className="mt-6 border border-slate-200 rounded-xl p-4 bg-slate-50">
                <div className="flex items-center justify-between gap-2 mb-2">
                  <label className="text-sm font-medium text-slate-700">Name</label>
                  {!isEditingName && (
                    <button
                      type="button"
                      onClick={() => {
                        setEditableName(profile?.name || "");
                        setSaveMsg({ type: "", text: "" });
                        setIsEditingName(true);
                      }}
                      className="text-sm text-indigo-600 hover:underline"
                    >
                      Edit
                    </button>
                  )}
                </div>
                {isEditingName ? (
                  <div className="flex flex-wrap gap-2">
                    <input
                      value={editableName}
                      onChange={(e) => setEditableName(e.target.value)}
                      className="flex-1 min-w-[220px] border border-slate-300 rounded-lg px-3 py-2 text-sm"
                      placeholder="Enter your name"
                    />
                    <button
                      type="button"
                      onClick={saveStudentName}
                      disabled={savingName}
                      className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 disabled:opacity-60"
                    >
                      {savingName ? "Saving..." : "Save"}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setIsEditingName(false);
                        setEditableName(profile?.name || "");
                        setSaveMsg({ type: "", text: "" });
                      }}
                      className="bg-slate-200 text-slate-800 px-4 py-2 rounded-lg hover:bg-slate-300"
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <p className="text-sm text-slate-900">{profile?.name || "-"}</p>
                )}
                {saveMsg.text && (
                  <p className={`mt-2 text-sm ${saveMsg.type === "ok" ? "text-emerald-700" : "text-red-700"}`}>
                    {saveMsg.text}
                  </p>
                )}
              </div>
            )}

            <div className="mt-6 border border-slate-200 rounded-xl overflow-hidden">
              {profileRows.map((item) => (
                <div key={item.label} className="grid grid-cols-3 border-b border-slate-100 last:border-b-0">
                  <div className="bg-slate-50 p-3 text-sm font-medium text-slate-700">{item.label}</div>
                  <div
                    className={`col-span-2 p-3 text-sm text-slate-900 ${
                      item.label === "Name" ? "font-bold" : ""
                    }`}
                  >
                    {item.value || "-"}
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {error && (
          <div className="mt-4 bg-red-50 border border-red-200 text-red-700 rounded-lg px-3 py-2 text-sm">{error}</div>
        )}

        <div className="mt-6 flex flex-wrap gap-3">
          <button
            type="button"
            onClick={() => navigate(dashboardPath)}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700"
          >
            Home page
          </button>
          <button
            type="button"
            onClick={() => {
              localStorage.removeItem("token");
              localStorage.removeItem("role");
              navigate("/");
            }}
            className="bg-slate-200 text-slate-800 px-4 py-2 rounded-lg hover:bg-slate-300"
          >
            Logout
          </button>
        </div>
      </div>
    </div>
  );
}

export default Profile;
