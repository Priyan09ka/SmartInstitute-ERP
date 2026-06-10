import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest } from "../../services/Api";

function localDateString(d = new Date()) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/**
 * @param {{ mode: 'admin' | 'principal' }} props
 */
function InstitutePortalDashboard({ mode }) {
  const isAdmin = mode === "admin";

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [institute, setInstitute] = useState(null);
  const [students, setStudents] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [principals, setPrincipals] = useState([]);
  const [teacherSubjects, setTeacherSubjects] = useState([]);

  const [attendanceDate, setAttendanceDate] = useState(() => localDateString());
  const [attendanceSummary, setAttendanceSummary] = useState(null);
  const [classroomId, setClassroomId] = useState("");
  const [classAttendance, setClassAttendance] = useState([]);

  const [principalForm, setPrincipalForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [principalMsg, setPrincipalMsg] = useState({ type: "", text: "" });
  const [savingPrincipal, setSavingPrincipal] = useState(false);

  const loadOverview = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const [inst, stu, tea, cls, prin, ts] = await Promise.all([
        apiRequest("/platform/institute-admin/my-institute"),
        apiRequest("/institute/students"),
        apiRequest("/api/teachers"),
        apiRequest("/institute/classes"),
        apiRequest("/api/principals"),
        apiRequest("/api/teacher-subjects"),
      ]);
      setInstitute(inst);
      setStudents(Array.isArray(stu) ? stu : []);
      setTeachers(Array.isArray(tea) ? tea : []);
      setClassrooms(Array.isArray(cls) ? cls : []);
      setPrincipals(Array.isArray(prin) ? prin : []);
      setTeacherSubjects(Array.isArray(ts) ? ts : []);
    } catch (e) {
      setError(e.message || "Failed to load data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOverview();
  }, [loadOverview]);

  const loadAttendanceSummary = useCallback(async () => {
    try {
      const s = await apiRequest(`/api/attendance/summary?date=${encodeURIComponent(attendanceDate)}`);
      setAttendanceSummary(s);
    } catch (e) {
      setAttendanceSummary(null);
    }
  }, [attendanceDate]);

  useEffect(() => {
    loadAttendanceSummary();
  }, [loadAttendanceSummary]);

  useEffect(() => {
    if (!classroomId || !attendanceDate) {
      setClassAttendance([]);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const rows = await apiRequest(
          `/api/attendance?classroomId=${encodeURIComponent(classroomId)}&date=${encodeURIComponent(attendanceDate)}`
        );
        if (!cancelled) setClassAttendance(Array.isArray(rows) ? rows : []);
      } catch {
        if (!cancelled) setClassAttendance([]);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [classroomId, attendanceDate]);

  const stats = useMemo(
    () => ({
      students: students.length,
      teachers: teachers.length,
      classrooms: classrooms.length,
      principals: principals.length,
      teachingAssignments: teacherSubjects.length,
    }),
    [students, teachers, classrooms, principals, teacherSubjects]
  );

  const handlePrincipalSubmit = async (e) => {
    e.preventDefault();
    setPrincipalMsg({ type: "", text: "" });
    const pwd = principalForm.password.trim();
    const confirm = principalForm.confirmPassword.trim();
    if (!principalForm.name?.trim() || !principalForm.email?.trim()) {
      setPrincipalMsg({ type: "err", text: "Name and email are required." });
      return;
    }
    if (pwd.length < 6) {
      setPrincipalMsg({ type: "err", text: "Password must be at least 6 characters." });
      return;
    }
    if (pwd !== confirm) {
      setPrincipalMsg({ type: "err", text: "Password and confirm password do not match." });
      return;
    }
    setSavingPrincipal(true);
    try {
      await apiRequest("/api/principals", "POST", {
        name: principalForm.name.trim(),
        email: principalForm.email.trim(),
        password: pwd,
        confirmPassword: confirm,
      });
      setPrincipalMsg({ type: "ok", text: "Principal created. They can sign in with this email and password." });
      setPrincipalForm({ name: "", email: "", password: "", confirmPassword: "" });
      await loadOverview();
    } catch (err) {
      setPrincipalMsg({ type: "err", text: err.message || "Could not create principal." });
    } finally {
      setSavingPrincipal(false);
    }
  };

  if (loading && !institute) {
    return (
      <div className="p-8 text-slate-600">Loading institute…</div>
    );
  }

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-8">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">
          {isAdmin ? "Institute admin" : "Principal"} overview
        </h1>
        <p className="text-slate-600 mt-1">
          {institute?.name ? (
            <>
              <span className="font-medium text-slate-800">{institute.name}</span>
              {institute.address ? <span className="text-slate-500"> — {institute.address}</span> : null}
            </>
          ) : (
            "Your institute"
          )}
        </p>
      </header>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      <section>
        <h2 className="text-lg font-semibold text-slate-800 mb-3">Activity snapshot</h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
          {[
            { label: "Students", value: stats.students },
            { label: "Teachers", value: stats.teachers },
            { label: "Classes", value: stats.classrooms },
            { label: "Principals", value: stats.principals },
            { label: "Teacher–subject links", value: stats.teachingAssignments },
          ].map((c) => (
            <div key={c.label} className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
              <div className="text-2xl font-bold text-indigo-700">{c.value}</div>
              <div className="text-xs text-slate-500 mt-1">{c.label}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-8">
          <div>
            <h2 className="text-lg font-semibold text-slate-800 mb-1">Students</h2>
            <p className="text-sm text-slate-600 mb-3">All students in your institute.</p>
            {students.length === 0 ? (
              <p className="text-slate-500 text-sm">No students yet.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-slate-600">
                      <th className="py-2 pr-4">Name</th>
                      <th className="py-2 pr-4">Email</th>
                      <th className="py-2 pr-4">Roll no.</th>
                      <th className="py-2">Class</th>
                    </tr>
                  </thead>
                  <tbody>
                    {students.map((s) => (
                      <tr key={s.id} className="border-b border-slate-100">
                        <td className="py-2 pr-4">{s.name}</td>
                        <td className="py-2 pr-4">{s.email || "—"}</td>
                        <td className="py-2 pr-4">{s.rollNumber || "—"}</td>
                        <td className="py-2">{s.classroomName || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div>
            <h2 className="text-lg font-semibold text-slate-800 mb-1">Teachers</h2>
            <p className="text-sm text-slate-600 mb-3">Teaching staff linked to your institute.</p>
            {teachers.length === 0 ? (
              <p className="text-slate-500 text-sm">No teachers yet.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-slate-600">
                      <th className="py-2 pr-4">Name</th>
                      <th className="py-2 pr-4">Email</th>
                      <th className="py-2 pr-4">Status</th>
                      <th className="py-2">Role</th>
                    </tr>
                  </thead>
                  <tbody>
                    {teachers.map((t) => (
                      <tr key={t.id} className="border-b border-slate-100">
                        <td className="py-2 pr-4">{t.name}</td>
                        <td className="py-2 pr-4">{t.email || "—"}</td>
                        <td className="py-2 pr-4">{t.status || "—"}</td>
                        <td className="py-2">{t.role || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div>
            <h2 className="text-lg font-semibold text-slate-800 mb-1">Principals</h2>
            <p className="text-sm text-slate-600 mb-3">Principals who can access this institute.</p>
            {principals.length === 0 ? (
              <p className="text-slate-500 text-sm">
                {isAdmin ? "No principals yet. Create one below." : "No principals yet."}
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-slate-600">
                      <th className="py-2 pr-4">Name</th>
                      <th className="py-2">Email</th>
                    </tr>
                  </thead>
                  <tbody>
                    {principals.map((p) => (
                      <tr key={p.id} className="border-b border-slate-100">
                        <td className="py-2 pr-4">{p.name}</td>
                        <td className="py-2">{p.email || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
      </section>

      {isAdmin && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-2">Create principal</h2>
          <p className="text-sm text-slate-600 mb-4">
            Principals can review the same institute-wide schedule and attendance as you.
          </p>
          <form onSubmit={handlePrincipalSubmit} className="grid sm:grid-cols-2 gap-4 max-w-2xl">
            {principalMsg.text && (
              <div
                className={`sm:col-span-2 text-sm px-3 py-2 rounded-lg ${
                  principalMsg.type === "ok"
                    ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                    : "bg-red-50 text-red-800 border border-red-200"
                }`}
              >
                {principalMsg.text}
              </div>
            )}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Full name</label>
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={principalForm.name}
                onChange={(e) => setPrincipalForm((p) => ({ ...p, name: e.target.value }))}
                placeholder="Principal name"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Email</label>
              <input
                type="email"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={principalForm.email}
                onChange={(e) => setPrincipalForm((p) => ({ ...p, email: e.target.value }))}
                placeholder="principal@school.edu"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
              <input
                type="password"
                autoComplete="new-password"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={principalForm.password}
                onChange={(e) => setPrincipalForm((p) => ({ ...p, password: e.target.value }))}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Confirm password</label>
              <input
                type="password"
                autoComplete="new-password"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={principalForm.confirmPassword}
                onChange={(e) => setPrincipalForm((p) => ({ ...p, confirmPassword: e.target.value }))}
              />
            </div>
            <div className="sm:col-span-2">
              <button
                type="submit"
                disabled={savingPrincipal}
                className="bg-indigo-600 text-white px-5 py-2 rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {savingPrincipal ? "Creating…" : "Create principal"}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-1">College schedule (teacher ↔ subject)</h2>
        <p className="text-sm text-slate-600 mb-4">
          Assignments for your institute (timetable slots can be added later on top of these links).
        </p>
        {teacherSubjects.length === 0 ? (
          <p className="text-slate-500 text-sm">No teacher–subject assignments yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-600">
                  <th className="py-2 pr-4">Teacher</th>
                  <th className="py-2">Subject</th>
                </tr>
              </thead>
              <tbody>
                {teacherSubjects.map((row) => (
                  <tr key={row.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{row.teacherName}</td>
                    <td className="py-2">{row.subjectName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Attendance</h2>
        <div className="flex flex-wrap gap-4 items-end mb-6">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Date</label>
            <input
              type="date"
              className="border border-slate-300 rounded-lg px-3 py-2"
              value={attendanceDate}
              onChange={(e) => setAttendanceDate(e.target.value)}
            />
          </div>
          {attendanceSummary && (
            <div className="flex flex-wrap gap-6 text-sm">
              <span>
                <span className="text-slate-500">Present: </span>
                <strong className="text-emerald-700">{attendanceSummary.presentCount}</strong>
              </span>
              <span>
                <span className="text-slate-500">Absent: </span>
                <strong className="text-rose-700">{attendanceSummary.absentCount}</strong>
              </span>
              <span>
                <span className="text-slate-500">Holiday: </span>
                <strong className="text-amber-700">{attendanceSummary.holidayCount ?? 0}</strong>
              </span>
              <span>
                <span className="text-slate-500">Total marked: </span>
                <strong>{attendanceSummary.totalMarked}</strong>
              </span>
            </div>
          )}
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-slate-700 mb-1">Class detail</label>
          <select
            className="border border-slate-300 rounded-lg px-3 py-2 max-w-md"
            value={classroomId}
            onChange={(e) => setClassroomId(e.target.value)}
          >
            <option value="">Select a class…</option>
            {classrooms.map((c) => (
              <option key={c.id} value={String(c.id)}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        {!classroomId ? (
          <p className="text-slate-500 text-sm">Choose a class to see student-level attendance for that date.</p>
        ) : classAttendance.length === 0 ? (
          <p className="text-slate-500 text-sm">No attendance records for this class and date.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-600">
                  <th className="py-2 pr-4">Student</th>
                  <th className="py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {classAttendance.map((row) => (
                  <tr key={row.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{row.studentName}</td>
                    <td className="py-2">{row.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default InstitutePortalDashboard;
