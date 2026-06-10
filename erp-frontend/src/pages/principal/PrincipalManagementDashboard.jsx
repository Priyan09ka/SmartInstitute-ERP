import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import InstitutePortalLayout from "../../components/InstitutePortalLayout";
import { apiRequest } from "../../services/Api";

const DAYS = [
  { v: "MON", l: "Monday" },
  { v: "TUE", l: "Tuesday" },
  { v: "WED", l: "Wednesday" },
  { v: "THU", l: "Thursday" },
  { v: "FRI", l: "Friday" },
  { v: "SAT", l: "Saturday" },
  { v: "SUN", l: "Sunday" },
];

/** All principal sections — navigation lives in the left sidebar only. */
const SIDEBAR_NAV = [
  { id: "overview", label: "Overview" },
  { id: "classes", label: "Classes" },
  { id: "classWise", label: "Class-wise" },
  { id: "fees", label: "Fees" },
  { id: "courses", label: "Courses" },
  { id: "subjects", label: "Subjects" },
  { id: "teachers", label: "Teachers" },
  { id: "teacherSubject", label: "Teacher ↔ Subject" },
  { id: "classTeacher", label: "Class teacher" },
  { id: "schedule", label: "Schedule" },
];

function Field({ label, children }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      {children}
    </div>
  );
}

export default function PrincipalManagementDashboard() {
  const [tab, setTab] = useState("overview");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [institute, setInstitute] = useState(null);

  const [courses, setCourses] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [teacherSubjects, setTeacherSubjects] = useState([]);
  const [classTeachers, setClassTeachers] = useState([]);
  const [scheduleSlots, setScheduleSlots] = useState([]);
  const [students, setStudents] = useState([]);
  const [fees, setFees] = useState([]);
  const [expandedFeeHistory, setExpandedFeeHistory] = useState({});
  const [feeHistoryByFeeId, setFeeHistoryByFeeId] = useState({});
  const [feeHistoryErrorByFeeId, setFeeHistoryErrorByFeeId] = useState({});
  const [feeHistoryLoadingByFeeId, setFeeHistoryLoadingByFeeId] = useState({});

  const [msg, setMsg] = useState({ type: "", text: "" });

  const notify = (type, text) => {
    setMsg({ type, text });
    if (text) setTimeout(() => setMsg({ type: "", text: "" }), 5000);
  };

  const loadAll = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const safeSchedule = async () => {
        try {
          return await apiRequest("/api/schedule");
        } catch {
          return [];
        }
      };
      const safeStudents = async () => {
        try {
          return await apiRequest("/institute/students");
        } catch {
          return [];
        }
      };
      const [inst, crs, cls, subj, tea, ts, ct, sch, stu, feeRows] = await Promise.all([
        apiRequest("/platform/institute-admin/my-institute"),
        apiRequest("/api/courses"),
        apiRequest("/institute/classes"),
        apiRequest("/api/subjects"),
        apiRequest("/api/teachers"),
        apiRequest("/api/teacher-subjects"),
        apiRequest("/api/class-teachers"),
        safeSchedule(),
        safeStudents(),
        apiRequest("/api/fees"),
      ]);
      setInstitute(inst);
      setCourses(Array.isArray(crs) ? crs : []);
      setClassrooms(Array.isArray(cls) ? cls : []);
      setSubjects(Array.isArray(subj) ? subj : []);
      setTeachers(Array.isArray(tea) ? tea : []);
      setTeacherSubjects(Array.isArray(ts) ? ts : []);
      setClassTeachers(Array.isArray(ct) ? ct : []);
      setScheduleSlots(Array.isArray(sch) ? sch : []);
      setStudents(Array.isArray(stu) ? stu : []);
      setFees(Array.isArray(feeRows) ? feeRows : []);
      setExpandedFeeHistory({});
      setFeeHistoryByFeeId({});
      setFeeHistoryErrorByFeeId({});
      setFeeHistoryLoadingByFeeId({});
    } catch (e) {
      setError(e.message || "Failed to load data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const stats = useMemo(
    () => ({
      courses: courses.length,
      classes: classrooms.length,
      subjects: subjects.length,
      teachers: teachers.length,
      teacherSubjectLinks: teacherSubjects.length,
      classTeachers: classTeachers.length,
      scheduleSlots: scheduleSlots.length,
      students: students.length,
    }),
    [courses, classrooms, subjects, teachers, teacherSubjects, classTeachers, scheduleSlots, students]
  );

  /** Per class: homeroom teacher, teachers from timetable, subjects (timetable + class teacher’s teacher–subject links), students. */
  const classWiseRows = useMemo(() => {
    return classrooms.map((c) => {
      const ct = classTeachers.find((x) => x.classroomId === c.id);
      const schedForClass = scheduleSlots.filter((s) => s.classroomId === c.id);
      const schedTeacherNames = [...new Set(schedForClass.map((s) => s.teacherName).filter(Boolean))];
      const schedSubjectNames = [...new Set(schedForClass.map((s) => s.subjectName).filter(Boolean))];

      let fromClassTeacherTs = [];
      if (ct?.teacherId) {
        fromClassTeacherTs = teacherSubjects
          .filter((ts) => ts.teacherId === ct.teacherId)
          .map((ts) => ts.subjectName);
      }
      const subjectSet = new Set([...schedSubjectNames, ...fromClassTeacherTs]);

      const classStudents = students.filter(
        (st) => st.classroomId === c.id || (st.classroomId == null && st.classroomName === c.name)
      );

      return {
        classroomId: c.id,
        name: c.name,
        classTeacherName: ct?.teacherName ?? "—",
        classTeacherId: ct?.teacherId ?? null,
        timetableTeachers: schedTeacherNames,
        subjects: [...subjectSet].sort(),
        students: classStudents,
      };
    });
  }, [classrooms, classTeachers, scheduleSlots, teacherSubjects, students]);

  // ---- forms state ----
  const [courseForm, setCourseForm] = useState({ name: "", code: "", description: "" });
  const [classForm, setClassForm] = useState({ name: "" });
  const [subjectForm, setSubjectForm] = useState({ name: "", code: "", courseId: "" });
  const [teacherForm, setTeacherForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [tsForm, setTsForm] = useState({ teacherId: "", subjectId: "" });
  const [ctForm, setCtForm] = useState({ classroomId: "", teacherId: "" });
  const [schForm, setSchForm] = useState({
    classroomId: "",
    teacherId: "",
    subjectId: "",
    dayOfWeek: "MON",
    startTime: "09:00",
    endTime: "10:00",
  });
  const [feeForm, setFeeForm] = useState({
    studentId: "",
    title: "",
    amount: "",
    dueDate: "",
  });

  const submitCourse = async (e) => {
    e.preventDefault();
    try {
      await apiRequest("/api/courses", "POST", {
        name: courseForm.name.trim(),
        code: courseForm.code.trim(),
        description: courseForm.description.trim() || null,
      });
      notify("ok", "Course created.");
      setCourseForm({ name: "", code: "", description: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitClass = async (e) => {
    e.preventDefault();
    try {
      await apiRequest("/institute/classes", "POST", { name: classForm.name.trim() });
      notify("ok", "Class created.");
      setClassForm({ name: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitSubject = async (e) => {
    e.preventDefault();
    if (!subjectForm.courseId) {
      notify("err", "Select a course.");
      return;
    }
    try {
      await apiRequest("/api/subjects", "POST", {
        name: subjectForm.name.trim(),
        code: subjectForm.code.trim(),
        courseId: Number(subjectForm.courseId),
      });
      notify("ok", "Subject created.");
      setSubjectForm({ name: "", code: "", courseId: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitTeacher = async (e) => {
    e.preventDefault();
    const pwd = teacherForm.password.trim();
    const c = teacherForm.confirmPassword.trim();
    if (pwd.length < 6) {
      notify("err", "Password must be at least 6 characters.");
      return;
    }
    if (pwd !== c) {
      notify("err", "Passwords do not match.");
      return;
    }
    try {
      await apiRequest("/api/teachers", "POST", {
        name: teacherForm.name.trim(),
        email: teacherForm.email.trim(),
        password: pwd,
        confirmPassword: c,
      });
      notify("ok", "Teacher created.");
      setTeacherForm({ name: "", email: "", password: "", confirmPassword: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitTeacherSubject = async (e) => {
    e.preventDefault();
    if (!tsForm.teacherId || !tsForm.subjectId) {
      notify("err", "Select teacher and subject.");
      return;
    }
    try {
      await apiRequest("/api/teacher-subjects", "POST", {
        teacherId: Number(tsForm.teacherId),
        subjectId: Number(tsForm.subjectId),
      });
      notify("ok", "Teacher assigned to subject.");
      setTsForm({ teacherId: "", subjectId: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitClassTeacher = async (e) => {
    e.preventDefault();
    if (!ctForm.classroomId || !ctForm.teacherId) {
      notify("err", "Select class and teacher.");
      return;
    }
    try {
      await apiRequest("/api/class-teachers", "POST", {
        classroomId: Number(ctForm.classroomId),
        teacherId: Number(ctForm.teacherId),
      });
      notify("ok", "Class teacher assigned.");
      setCtForm({ classroomId: "", teacherId: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitSchedule = async (e) => {
    e.preventDefault();
    if (!schForm.classroomId || !schForm.teacherId || !schForm.subjectId) {
      notify("err", "Select class, teacher, and subject.");
      return;
    }
    try {
      await apiRequest("/api/schedule", "POST", {
        classroomId: Number(schForm.classroomId),
        teacherId: Number(schForm.teacherId),
        subjectId: Number(schForm.subjectId),
        dayOfWeek: schForm.dayOfWeek,
        startTime: schForm.startTime,
        endTime: schForm.endTime,
      });
      notify("ok", "Schedule slot added.");
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const deleteSchedule = async (id) => {
    if (!window.confirm("Remove this schedule slot?")) return;
    try {
      await apiRequest(`/api/schedule/${id}`, "DELETE");
      notify("ok", "Removed.");
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const submitFee = async (e) => {
    e.preventDefault();
    if (!feeForm.studentId || !feeForm.title.trim() || !feeForm.amount || !feeForm.dueDate) {
      notify("err", "Select student, title, amount, and due date.");
      return;
    }
    try {
      await apiRequest("/api/fees", "POST", {
        studentId: Number(feeForm.studentId),
        title: feeForm.title.trim(),
        amount: Number(feeForm.amount),
        dueDate: feeForm.dueDate,
      });
      notify("ok", "Fee record added.");
      setFeeForm({ studentId: "", title: "", amount: "", dueDate: "" });
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const markFeePaid = async (id) => {
    if (!window.confirm("Mark this fee as paid?")) return;
    try {
      await apiRequest(`/api/fees/${id}/mark-paid`, "PATCH");
      notify("ok", "Fee marked as paid.");
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const addPayment = async (fee) => {
    const amountRaw = window.prompt("Enter payment amount:", String(fee.balanceAmount ?? ""));
    if (!amountRaw) return;
    const amount = Number(amountRaw);
    if (!Number.isFinite(amount) || amount <= 0) {
      notify("err", "Enter a valid payment amount.");
      return;
    }
    const methodRaw = window.prompt("Payment method (CASH/UPI/CARD/NET_BANKING/OTHER):", "CASH");
    const method = String(methodRaw || "CASH").trim().toUpperCase();
    const allowed = ["CASH", "UPI", "CARD", "NET_BANKING", "OTHER"];
    if (!allowed.includes(method)) {
      notify("err", "Invalid payment method.");
      return;
    }
    try {
      await apiRequest(`/api/fees/${fee.id}/pay`, "PATCH", { amount, method });
      notify("ok", "Payment recorded.");
      await loadAll();
    } catch (err) {
      notify("err", err.message);
    }
  };

  const toggleFeeHistory = async (feeId) => {
    const isOpen = !!expandedFeeHistory[feeId];
    if (isOpen) {
      setExpandedFeeHistory((s) => ({ ...s, [feeId]: false }));
      return;
    }
    setExpandedFeeHistory((s) => ({ ...s, [feeId]: true }));
    if (feeHistoryByFeeId[feeId]) return;
    setFeeHistoryLoadingByFeeId((s) => ({ ...s, [feeId]: true }));
    setFeeHistoryErrorByFeeId((s) => ({ ...s, [feeId]: "" }));
    try {
      const rows = await apiRequest(`/api/fees/${feeId}/payments`);
      setFeeHistoryByFeeId((s) => ({ ...s, [feeId]: Array.isArray(rows) ? rows : [] }));
    } catch (err) {
      setFeeHistoryErrorByFeeId((s) => ({ ...s, [feeId]: err.message || "Could not load payment history." }));
    } finally {
      setFeeHistoryLoadingByFeeId((s) => ({ ...s, [feeId]: false }));
    }
  };

  const downloadFromApi = async (url, fileName) => {
    const token = localStorage.getItem("token");
    const res = await fetch(`http://localhost:8081${url}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      credentials: "include",
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || "Download failed");
    }
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = href;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  };

  return (
    <InstitutePortalLayout
      portalTitle="Principal"
      sidebarNav={SIDEBAR_NAV}
      activeNavId={tab}
      onNavClick={setTab}
    >
      {loading && !institute ? (
        <div className="p-8 text-slate-600">Loading…</div>
      ) : (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Principal — academic setup</h1>
        <p className="text-slate-600 mt-1">
          {institute?.name ? (
            <span className="font-medium text-slate-800">{institute.name}</span>
          ) : (
            "Your institute"
          )}
        </p>
      </header>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm">{error}</div>
      )}

      {msg.text && (
        <div
          className={`px-4 py-3 rounded-lg text-sm ${
            msg.type === "ok"
              ? "bg-emerald-50 text-emerald-900 border border-emerald-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {msg.text}
        </div>
      )}

      {tab === "overview" && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            ["Courses", stats.courses],
            ["Classes", stats.classes],
            ["Students", stats.students],
            ["Subjects", stats.subjects],
            ["Teachers", stats.teachers],
            ["Teacher–subject", stats.teacherSubjectLinks],
            ["Class teachers", stats.classTeachers],
            ["Schedule slots", stats.scheduleSlots],
          ].map(([label, n]) => (
            <div key={label} className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
              <div className="text-2xl font-bold text-indigo-700">{n}</div>
              <div className="text-xs text-slate-500 mt-1">{label}</div>
            </div>
          ))}
        </div>
      )}

      {tab === "classWise" && (
        <section className="space-y-4">
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
            <h2 className="text-lg font-semibold text-slate-800">Class-wise overview</h2>
            <p className="text-sm text-slate-600 mt-1">
              For each class: <strong>class teacher</strong> (homeroom), <strong>teachers</strong> appearing in the
              timetable, <strong>subjects</strong> (from timetable and from the class teacher’s subject assignments), and{" "}
              <strong>students</strong> in that class.
            </p>
          </div>

          {classrooms.length === 0 ? (
            <p className="text-slate-500 text-sm">Add classes first (Classes tab).</p>
          ) : (
            <div className="space-y-6">
              {classWiseRows.map((row) => (
                <div
                  key={row.classroomId}
                  className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden"
                >
                  <div className="bg-slate-50 px-4 py-3 border-b border-slate-200 flex flex-wrap justify-between gap-2 items-center">
                    <h3 className="text-base font-semibold text-slate-900">{row.name}</h3>
                    <span className="text-xs text-slate-500">
                      {row.students.length} student{row.students.length === 1 ? "" : "s"}
                    </span>
                  </div>

                  <div className="p-4 grid md:grid-cols-2 gap-6 text-sm">
                    <div>
                      <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-2">Teachers</h4>
                      <p className="text-slate-800">
                        <span className="text-slate-500">Class teacher: </span>
                        {row.classTeacherName}
                      </p>
                      {row.timetableTeachers.length > 0 && (
                        <p className="text-slate-700 mt-2">
                          <span className="text-slate-500">In timetable: </span>
                          {row.timetableTeachers.join(", ")}
                        </p>
                      )}
                      {row.timetableTeachers.length === 0 && row.classTeacherName === "—" && (
                        <p className="text-amber-700 text-xs mt-1">Assign a class teacher and/or add schedule slots.</p>
                      )}
                    </div>

                    <div>
                      <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-2">Subjects</h4>
                      {row.subjects.length === 0 ? (
                        <p className="text-slate-500">No subjects linked yet (timetable or teacher–subject for class teacher).</p>
                      ) : (
                        <ul className="list-disc list-inside text-slate-800 space-y-0.5">
                          {row.subjects.map((sub) => (
                            <li key={sub}>{sub}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>

                  <div className="px-4 pb-4">
                    <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-2">Students</h4>
                    {row.students.length === 0 ? (
                      <p className="text-slate-500 text-sm">No students in this class.</p>
                    ) : (
                      <div className="overflow-x-auto border border-slate-100 rounded-lg">
                        <table className="min-w-full text-sm">
                          <thead>
                            <tr className="bg-slate-50 text-left text-slate-600 border-b border-slate-100">
                              <th className="py-2 px-3">Name</th>
                              <th className="py-2 px-3">Email</th>
                              <th className="py-2 px-3">Roll no.</th>
                            </tr>
                          </thead>
                          <tbody>
                            {row.students.map((s) => (
                              <tr key={s.id} className="border-b border-slate-50">
                                <td className="py-2 px-3">{s.name}</td>
                                <td className="py-2 px-3">{s.email || "—"}</td>
                                <td className="py-2 px-3">{s.rollNumber || "—"}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {tab === "courses" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Add course</h2>
          <form onSubmit={submitCourse} className="grid sm:grid-cols-2 gap-4 max-w-2xl">
            <Field label="Name *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={courseForm.name}
                onChange={(e) => setCourseForm((f) => ({ ...f, name: e.target.value }))}
                required
              />
            </Field>
            <Field label="Code *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={courseForm.code}
                onChange={(e) => setCourseForm((f) => ({ ...f, code: e.target.value }))}
                required
              />
            </Field>
            <Field label="Description">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2 sm:col-span-2"
                value={courseForm.description}
                onChange={(e) => setCourseForm((f) => ({ ...f, description: e.target.value }))}
              />
            </Field>
            <div className="sm:col-span-2">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Add course
              </button>
            </div>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Name</th>
                  <th className="py-2 pr-4">Code</th>
                  <th className="py-2">Description</th>
                </tr>
              </thead>
              <tbody>
                {courses.map((c) => (
                  <tr key={c.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{c.name}</td>
                    <td className="py-2 pr-4">{c.code}</td>
                    <td className="py-2">{c.description || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {courses.length === 0 && <p className="text-slate-500 text-sm py-4">No courses yet.</p>}
          </div>
        </section>
      )}

      {tab === "classes" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Add class</h2>
          <form onSubmit={submitClass} className="flex flex-wrap gap-4 items-end max-w-xl">
            <Field label="Class name *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2 min-w-[240px]"
                value={classForm.name}
                onChange={(e) => setClassForm({ name: e.target.value })}
                placeholder="e.g. FY BCA A"
                required
              />
            </Field>
            <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
              Add class
            </button>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2">Name</th>
                </tr>
              </thead>
              <tbody>
                {classrooms.map((c) => (
                  <tr key={c.id} className="border-b border-slate-100">
                    <td className="py-2">{c.name}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {classrooms.length === 0 && <p className="text-slate-500 text-sm py-4">No classes yet.</p>}
          </div>
        </section>
      )}

      {tab === "subjects" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Add subject</h2>
          <p className="text-sm text-slate-600">Subjects belong to a course. Create at least one course first.</p>
          <form onSubmit={submitSubject} className="grid sm:grid-cols-2 gap-4 max-w-2xl">
            <Field label="Course *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={subjectForm.courseId}
                onChange={(e) => setSubjectForm((f) => ({ ...f, courseId: e.target.value }))}
                required
              >
                <option value="">Select course…</option>
                {courses.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.name} ({c.code})
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Subject name *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={subjectForm.name}
                onChange={(e) => setSubjectForm((f) => ({ ...f, name: e.target.value }))}
                required
              />
            </Field>
            <Field label="Subject code *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={subjectForm.code}
                onChange={(e) => setSubjectForm((f) => ({ ...f, code: e.target.value }))}
                required
              />
            </Field>
            <div className="sm:col-span-2">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Add subject
              </button>
            </div>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Name</th>
                  <th className="py-2 pr-4">Code</th>
                  <th className="py-2">Course</th>
                </tr>
              </thead>
              <tbody>
                {subjects.map((s) => (
                  <tr key={s.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{s.name}</td>
                    <td className="py-2 pr-4">{s.code}</td>
                    <td className="py-2">{s.courseName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {subjects.length === 0 && <p className="text-slate-500 text-sm py-4">No subjects yet.</p>}
          </div>
        </section>
      )}

      {tab === "teachers" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Add teacher</h2>
          <form onSubmit={submitTeacher} className="grid sm:grid-cols-2 gap-4 max-w-2xl">
            <Field label="Full name *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={teacherForm.name}
                onChange={(e) => setTeacherForm((f) => ({ ...f, name: e.target.value }))}
                required
              />
            </Field>
            <Field label="Email *">
              <input
                type="email"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={teacherForm.email}
                onChange={(e) => setTeacherForm((f) => ({ ...f, email: e.target.value }))}
                required
              />
            </Field>
            <Field label="Password *">
              <input
                type="password"
                autoComplete="new-password"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={teacherForm.password}
                onChange={(e) => setTeacherForm((f) => ({ ...f, password: e.target.value }))}
              />
            </Field>
            <Field label="Confirm password *">
              <input
                type="password"
                autoComplete="new-password"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={teacherForm.confirmPassword}
                onChange={(e) => setTeacherForm((f) => ({ ...f, confirmPassword: e.target.value }))}
              />
            </Field>
            <div className="sm:col-span-2">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Add teacher
              </button>
            </div>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Name</th>
                  <th className="py-2 pr-4">Email</th>
                  <th className="py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {teachers.map((t) => (
                  <tr key={t.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{t.name}</td>
                    <td className="py-2 pr-4">{t.email}</td>
                    <td className="py-2">{t.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {teachers.length === 0 && <p className="text-slate-500 text-sm py-4">No teachers yet.</p>}
          </div>
        </section>
      )}

      {tab === "teacherSubject" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Assign teacher to subject</h2>
          <form onSubmit={submitTeacherSubject} className="grid sm:grid-cols-2 gap-4 max-w-2xl items-end">
            <Field label="Teacher *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={tsForm.teacherId}
                onChange={(e) => setTsForm((f) => ({ ...f, teacherId: e.target.value }))}
              >
                <option value="">Select…</option>
                {teachers.map((t) => (
                  <option key={t.id} value={String(t.id)}>
                    {t.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Subject *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={tsForm.subjectId}
                onChange={(e) => setTsForm((f) => ({ ...f, subjectId: e.target.value }))}
              >
                <option value="">Select…</option>
                {subjects.map((s) => (
                  <option key={s.id} value={String(s.id)}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
            </Field>
            <div className="sm:col-span-2">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Assign
              </button>
            </div>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Teacher</th>
                  <th className="py-2">Subject</th>
                </tr>
              </thead>
              <tbody>
                {teacherSubjects.map((r) => (
                  <tr key={r.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{r.teacherName}</td>
                    <td className="py-2">{r.subjectName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {teacherSubjects.length === 0 && (
              <p className="text-slate-500 text-sm py-4">No assignments yet.</p>
            )}
          </div>
        </section>
      )}

      {tab === "classTeacher" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Assign class teacher</h2>
          <p className="text-sm text-slate-600">One homeroom teacher per class (duplicate class will fail).</p>
          <form onSubmit={submitClassTeacher} className="grid sm:grid-cols-2 gap-4 max-w-2xl items-end">
            <Field label="Class *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={ctForm.classroomId}
                onChange={(e) => setCtForm((f) => ({ ...f, classroomId: e.target.value }))}
              >
                <option value="">Select…</option>
                {classrooms.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Teacher *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={ctForm.teacherId}
                onChange={(e) => setCtForm((f) => ({ ...f, teacherId: e.target.value }))}
              >
                <option value="">Select…</option>
                {teachers.map((t) => (
                  <option key={t.id} value={String(t.id)}>
                    {t.name}
                  </option>
                ))}
              </select>
            </Field>
            <div className="sm:col-span-2">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Assign
              </button>
            </div>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Class</th>
                  <th className="py-2">Teacher</th>
                </tr>
              </thead>
              <tbody>
                {classTeachers.map((r) => (
                  <tr key={r.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{r.classroomName}</td>
                    <td className="py-2">{r.teacherName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {classTeachers.length === 0 && <p className="text-slate-500 text-sm py-4">No class teachers yet.</p>}
          </div>
        </section>
      )}

      {tab === "schedule" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Weekly schedule</h2>
          <p className="text-sm text-slate-600">
            Add time slots: which class, teacher, and subject meet on a given day.
          </p>
          <form onSubmit={submitSchedule} className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 max-w-4xl">
            <Field label="Class *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={schForm.classroomId}
                onChange={(e) => setSchForm((f) => ({ ...f, classroomId: e.target.value }))}
              >
                <option value="">Select…</option>
                {classrooms.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Teacher *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={schForm.teacherId}
                onChange={(e) => setSchForm((f) => ({ ...f, teacherId: e.target.value }))}
              >
                <option value="">Select…</option>
                {teachers.map((t) => (
                  <option key={t.id} value={String(t.id)}>
                    {t.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Subject *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={schForm.subjectId}
                onChange={(e) => setSchForm((f) => ({ ...f, subjectId: e.target.value }))}
              >
                <option value="">Select…</option>
                {subjects.map((s) => (
                  <option key={s.id} value={String(s.id)}>
                    {s.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Day *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={schForm.dayOfWeek}
                onChange={(e) => setSchForm((f) => ({ ...f, dayOfWeek: e.target.value }))}
              >
                {DAYS.map((d) => (
                  <option key={d.v} value={d.v}>
                    {d.l}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Start (HH:mm) *">
              <input
                type="time"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={schForm.startTime.length === 5 ? schForm.startTime : schForm.startTime.slice(0, 5)}
                onChange={(e) => setSchForm((f) => ({ ...f, startTime: e.target.value }))}
              />
            </Field>
            <Field label="End (HH:mm) *">
              <input
                type="time"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={schForm.endTime.length === 5 ? schForm.endTime : schForm.endTime.slice(0, 5)}
                onChange={(e) => setSchForm((f) => ({ ...f, endTime: e.target.value }))}
              />
            </Field>
            <div className="sm:col-span-2 lg:col-span-3">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Add slot
              </button>
            </div>
          </form>
          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Day</th>
                  <th className="py-2 pr-4">Time</th>
                  <th className="py-2 pr-4">Class</th>
                  <th className="py-2 pr-4">Teacher</th>
                  <th className="py-2 pr-4">Subject</th>
                  <th className="py-2"> </th>
                </tr>
              </thead>
              <tbody>
                {scheduleSlots.map((r) => (
                  <tr key={r.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{r.dayOfWeek}</td>
                    <td className="py-2 pr-4">
                      {r.startTime?.slice?.(0, 5) ?? r.startTime} – {r.endTime?.slice?.(0, 5) ?? r.endTime}
                    </td>
                    <td className="py-2 pr-4">{r.classroomName}</td>
                    <td className="py-2 pr-4">{r.teacherName}</td>
                    <td className="py-2 pr-4">{r.subjectName}</td>
                    <td className="py-2">
                      <button
                        type="button"
                        onClick={() => deleteSchedule(r.id)}
                        className="text-red-600 hover:underline text-xs"
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {scheduleSlots.length === 0 && <p className="text-slate-500 text-sm py-4">No schedule slots yet.</p>}
          </div>
        </section>
      )}

      {tab === "fees" && (
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Fees management</h2>
          <p className="text-sm text-slate-600">Assign fees to students and track payment status.</p>
          <div className="flex flex-wrap gap-4 text-sm">
            <button
              type="button"
              onClick={async () => {
                try {
                  await downloadFromApi("/api/fees/export", "fees-export.csv");
                } catch (e) {
                  notify("err", e?.message || "Could not export all fees.");
                }
              }}
              className="text-indigo-600 hover:underline"
            >
              Export all fees (CSV)
            </button>
            <button
              type="button"
              onClick={async () => {
                try {
                  await downloadFromApi("/api/fees/export?status=PAID", "paid-fees-export.csv");
                } catch (e) {
                  notify("err", e?.message || "Could not export paid fees.");
                }
              }}
              className="text-indigo-600 hover:underline"
            >
              Export paid fees (CSV)
            </button>
          </div>
          <form onSubmit={submitFee} className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Field label="Student *">
              <select
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={feeForm.studentId}
                onChange={(e) => setFeeForm((f) => ({ ...f, studentId: e.target.value }))}
              >
                <option value="">Select…</option>
                {students.map((s) => (
                  <option key={s.id} value={String(s.id)}>
                    {s.name} {s.classroomName ? `(${s.classroomName})` : ""}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Title *">
              <input
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={feeForm.title}
                onChange={(e) => setFeeForm((f) => ({ ...f, title: e.target.value }))}
                placeholder="e.g. Term 1 Fee"
              />
            </Field>
            <Field label="Amount *">
              <input
                type="number"
                min="0"
                step="0.01"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={feeForm.amount}
                onChange={(e) => setFeeForm((f) => ({ ...f, amount: e.target.value }))}
              />
            </Field>
            <Field label="Due date *">
              <input
                type="date"
                className="w-full border border-slate-300 rounded-lg px-3 py-2"
                value={feeForm.dueDate}
                onChange={(e) => setFeeForm((f) => ({ ...f, dueDate: e.target.value }))}
              />
            </Field>
            <div className="sm:col-span-2 lg:col-span-4">
              <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium">
                Add fee
              </button>
            </div>
          </form>

          <div className="overflow-x-auto pt-4 border-t border-slate-100">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left text-slate-600 border-b">
                  <th className="py-2 pr-4">Student</th>
                  <th className="py-2 pr-4">Title</th>
                  <th className="py-2 pr-4">Due date</th>
                  <th className="py-2 pr-4">Total</th>
                  <th className="py-2 pr-4">Paid</th>
                  <th className="py-2 pr-4">Balance</th>
                  <th className="py-2 pr-4">Status</th>
                  <th className="py-2 pr-4">Method</th>
                  <th className="py-2 pr-4">Receipt</th>
                  <th className="py-2">Action</th>
                </tr>
              </thead>
              <tbody>
                {fees.map((f) => (
                  <Fragment key={f.id}>
                    <tr className="border-b border-slate-100">
                      <td className="py-2 pr-4">
                        {f.studentName || "—"}
                        {f.rollNumber ? <span className="text-slate-500"> · Roll {f.rollNumber}</span> : null}
                      </td>
                      <td className="py-2 pr-4">{f.title}</td>
                      <td className="py-2 pr-4">{f.dueDate}</td>
                      <td className="py-2 pr-4">Rs. {Number(f.amount || 0).toFixed(2)}</td>
                      <td className="py-2 pr-4">Rs. {Number(f.amountPaid || 0).toFixed(2)}</td>
                      <td className="py-2 pr-4">Rs. {Number(f.balanceAmount || 0).toFixed(2)}</td>
                      <td className="py-2 pr-4">
                        <span className={f.status === "PAID" ? "text-emerald-700 font-medium" : "text-rose-700 font-medium"}>
                          {f.status}
                        </span>
                      </td>
                      <td className="py-2 pr-4">{f.lastPaymentMethod || "—"}</td>
                      <td className="py-2 pr-4">{f.receiptNumber || "—"}</td>
                      <td className="py-2">
                        <div className="flex flex-wrap gap-3">
                          {f.status !== "PAID" ? (
                            <>
                              <button
                                type="button"
                                onClick={() => addPayment(f)}
                                className="text-indigo-600 hover:underline text-xs"
                              >
                                Add payment
                              </button>
                              <button
                                type="button"
                                onClick={() => markFeePaid(f.id)}
                                className="text-indigo-600 hover:underline text-xs"
                              >
                                Mark full paid
                              </button>
                            </>
                          ) : (
                            <button
                              type="button"
                              onClick={async () => {
                                try {
                                  await downloadFromApi(`/api/fees/${f.id}/receipt`, `fee-receipt-${f.id}.pdf`);
                                } catch (e) {
                                  notify("err", e?.message || "Could not download receipt.");
                                }
                              }}
                              className="text-indigo-600 hover:underline text-xs"
                            >
                              Download receipt
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => toggleFeeHistory(f.id)}
                            className="text-indigo-600 hover:underline text-xs"
                          >
                            {expandedFeeHistory[f.id] ? "Hide payments" : "View payments"}
                          </button>
                        </div>
                      </td>
                    </tr>
                    {expandedFeeHistory[f.id] ? (
                      <tr className="bg-slate-50 border-b border-slate-100">
                        <td colSpan={10} className="px-3 py-3">
                          {feeHistoryLoadingByFeeId[f.id] ? (
                            <p className="text-xs text-slate-500">Loading payments...</p>
                          ) : feeHistoryErrorByFeeId[f.id] ? (
                            <p className="text-xs text-rose-700">{feeHistoryErrorByFeeId[f.id]}</p>
                          ) : (feeHistoryByFeeId[f.id] || []).length === 0 ? (
                            <p className="text-xs text-slate-500">No payments recorded yet.</p>
                          ) : (
                            <div className="space-y-2">
                              {(feeHistoryByFeeId[f.id] || []).map((p) => (
                                <div
                                  key={p.id}
                                  className="text-xs border border-slate-200 rounded-md bg-white px-3 py-2 flex flex-wrap gap-4"
                                >
                                  <span><strong>Amount:</strong> Rs. {Number(p.amount || 0).toFixed(2)}</span>
                                  <span><strong>Method:</strong> {p.method || "—"}</span>
                                  <span><strong>Receipt:</strong> {p.receiptNumber || "—"}</span>
                                  <span><strong>Paid:</strong> {p.paidAt ? new Date(p.paidAt).toLocaleString() : "—"}</span>
                                  {p.note ? <span><strong>Note:</strong> {p.note}</span> : null}
                                </div>
                              ))}
                            </div>
                          )}
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                ))}
              </tbody>
            </table>
            {fees.length === 0 && <p className="text-slate-500 text-sm py-4">No fee records yet.</p>}
          </div>
        </section>
      )}

      <section className="bg-slate-50 border border-slate-200 rounded-lg p-4 text-sm text-slate-600">
        <strong className="text-slate-800">Suggested order:</strong> create courses → classes & subjects → teachers →
        link teachers to subjects → assign class teachers → build the weekly schedule.
      </section>
    </div>
      )}
    </InstitutePortalLayout>
  );
}
