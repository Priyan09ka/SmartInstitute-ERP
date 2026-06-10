import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import InstitutePortalLayout from "../../components/InstitutePortalLayout";
import { apiRequest, apiUploadMultipart } from "../../services/Api";

function localDateString(d = new Date()) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function TeacherDashboard() {
  const [activeNavId, setActiveNavId] = useState("overview");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [me, setMe] = useState(null);
  const [classrooms, setClassrooms] = useState([]);
  const [students, setStudents] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  /** Subjects for the selected class (from timetable); loaded when creating an exam. */
  const [quizSubjects, setQuizSubjects] = useState([]);
  const [quizSubjectsLoading, setQuizSubjectsLoading] = useState(false);

  // —— Attendance ——
  const [attClassId, setAttClassId] = useState("");
  const [attDate, setAttDate] = useState(() => localDateString());
  /** @type {Record<string, 'PRESENT' | 'ABSENT' | 'HOLIDAY'>} */
  const [attStatusByStudent, setAttStatusByStudent] = useState({});
  const [attLoading, setAttLoading] = useState(false);
  const [attMsg, setAttMsg] = useState({ type: "", text: "" });

  // —— Exams ——
  const [quizForm, setQuizForm] = useState({
    title: "",
    totalMarks: 20,
    durationMinutes: 45,
    subjectId: "",
    classroomId: "",
  });
  const [quizMsg, setQuizMsg] = useState({ type: "", text: "" });
  const [savingQuiz, setSavingQuiz] = useState(false);

  const [selectedQuizId, setSelectedQuizId] = useState("");
  const [questionForm, setQuestionForm] = useState({
    question: "",
    optionA: "",
    optionB: "",
    optionC: "",
    optionD: "",
    correctAnswer: "",
    marks: 1,
  });
  const [qMsg, setQMsg] = useState({ type: "", text: "" });
  const [savingQ, setSavingQ] = useState(false);

  const [importCsvKey, setImportCsvKey] = useState(0);
  const [importPdfKey, setImportPdfKey] = useState(0);
  const [importBusy, setImportBusy] = useState(false);
  const [importFeedback, setImportFeedback] = useState(null);

  const [uploadClassId, setUploadClassId] = useState("");
  const [studentUploadKey, setStudentUploadKey] = useState(0);
  const [studentUploadBusy, setStudentUploadBusy] = useState(false);
  const [studentUploadMsg, setStudentUploadMsg] = useState({ type: "", text: "" });
  const studentUploadInFlight = useRef(false);

  const homeroomIds = useMemo(() => {
    const h = me?.homeroomClassroomIds;
    if (!Array.isArray(h)) return [];
    return h.map((x) => Number(x));
  }, [me]);

  const isHomeroomTeacher = homeroomIds.length > 0;
  const isSubjectTeacher = !!me?.assignedAsSubjectTeacher;

  /** Class teacher vs subject teacher are different responsibilities; UI copy and nav follow this. */
  const teacherMode = useMemo(() => {
    if (isHomeroomTeacher && isSubjectTeacher) return "both";
    if (isHomeroomTeacher) return "class_only";
    if (isSubjectTeacher) return "subject_only";
    return "other";
  }, [isHomeroomTeacher, isSubjectTeacher]);

  const portalTitle = useMemo(() => {
    switch (teacherMode) {
      case "class_only":
        return "Class teacher";
      case "subject_only":
        return "Subject teacher";
      case "both":
        return "Class & subject";
      default:
        return "Teacher";
    }
  }, [teacherMode]);

  const portalTagline = useMemo(() => {
    switch (teacherMode) {
      case "class_only":
        return "Homeroom class, student roster, class-level tasks";
      case "subject_only":
        return "Subjects you teach, timetable slots, exams";
      case "both":
        return "Homeroom duties and subject teaching — separate areas below";
      default:
        return "Institute portal";
    }
  }, [teacherMode]);

  const mainDashboardTitle = useMemo(() => {
    switch (teacherMode) {
      case "class_only":
        return "Class teacher dashboard";
      case "subject_only":
        return "Subject teacher dashboard";
      case "both":
        return "Teacher dashboard";
      default:
        return "Teacher dashboard";
    }
  }, [teacherMode]);

  const examNavLabel = useMemo(() => {
    if (teacherMode === "subject_only") return "Exams (your subjects)";
    if (teacherMode === "class_only") return "Exams (your classes)";
    return "Exams";
  }, [teacherMode]);

  const homeroomClassrooms = useMemo(
    () => classrooms.filter((c) => homeroomIds.includes(Number(c.id))),
    [classrooms, homeroomIds]
  );

  const navItems = useMemo(() => {
    const items = [{ id: "overview", label: "Overview" }];
    if (isHomeroomTeacher) items.push({ id: "my-class", label: "Homeroom & students" });
    if (classrooms.length > 0) {
      items.push({
        id: "attendance",
        label: teacherMode === "subject_only" ? "Attendance (your classes)" : "Attendance",
      });
    }
    if (isHomeroomTeacher || isSubjectTeacher) items.push({ id: "exams", label: examNavLabel });
    return items;
  }, [isHomeroomTeacher, isSubjectTeacher, classrooms.length, teacherMode, examNavLabel]);

  useEffect(() => {
    if (navItems.length > 0 && !navItems.some((n) => n.id === activeNavId)) {
      setActiveNavId(navItems[0].id);
    }
  }, [navItems, activeNavId]);

  useEffect(() => {
    if (!uploadClassId && homeroomClassrooms.length > 0) {
      setUploadClassId(String(homeroomClassrooms[0].id));
    }
  }, [homeroomClassrooms, uploadClassId]);

  const homeroomStudents = useMemo(() => {
    if (!homeroomIds.length) return [];
    const set = new Set(homeroomIds);
    return students.filter((s) => set.has(Number(s.classroomId)));
  }, [students, homeroomIds]);

  const loadCore = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const [m, cls, stu, qz] = await Promise.all([
        apiRequest("/api/teachers/me"),
        apiRequest("/institute/classes"),
        apiRequest("/institute/students"),
        apiRequest("/api/quizzes"),
      ]);
      setMe(m);
      setClassrooms(Array.isArray(cls) ? cls : []);
      setStudents(Array.isArray(stu) ? stu : []);
      setQuizzes(Array.isArray(qz) ? qz : []);
    } catch (e) {
      setError(e.message || "Failed to load data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCore();
  }, [loadCore]);

  useEffect(() => {
    if (!quizForm.classroomId) {
      setQuizSubjects([]);
      return;
    }
    let cancelled = false;
    setQuizSubjectsLoading(true);
    (async () => {
      try {
        const list = await apiRequest(
          `/api/subjects?classroomId=${encodeURIComponent(quizForm.classroomId)}`
        );
        if (!cancelled) setQuizSubjects(Array.isArray(list) ? list : []);
      } catch {
        if (!cancelled) setQuizSubjects([]);
      } finally {
        if (!cancelled) setQuizSubjectsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [quizForm.classroomId]);

  const studentsInClass = useMemo(() => {
    if (!attClassId) return [];
    const cid = Number(attClassId);
    return students.filter((s) => s.classroomId === cid);
  }, [students, attClassId]);

  useEffect(() => {
    if (!attClassId || !attDate) {
      setAttStatusByStudent({});
      return;
    }
    const cid = Number(attClassId);
    let cancelled = false;
    (async () => {
      setAttLoading(true);
      try {
        const rows = await apiRequest(
          `/api/attendance?classroomId=${encodeURIComponent(attClassId)}&date=${encodeURIComponent(attDate)}`
        );
        const list = Array.isArray(rows) ? rows : [];
        const fromApi = {};
        for (const r of list) {
          if (r.studentId != null) fromApi[String(r.studentId)] = r.status;
        }
        const inClass = students.filter((x) => x.classroomId === cid);
        const next = {};
        for (const s of inClass) {
          const key = String(s.id);
          next[key] = fromApi[key] != null ? fromApi[key] : "PRESENT";
        }
        if (!cancelled) setAttStatusByStudent(next);
      } catch {
        if (!cancelled) {
          const inClass = students.filter((x) => x.classroomId === cid);
          const next = {};
          for (const s of inClass) next[String(s.id)] = "PRESENT";
          setAttStatusByStudent(next);
        }
      } finally {
        if (!cancelled) setAttLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [attClassId, attDate, students]);

  const setStatus = (studentId, status) => {
    setAttStatusByStudent((prev) => ({ ...prev, [String(studentId)]: status }));
  };

  const markAllHoliday = () => {
    if (studentsInClass.length === 0) return;
    const next = { ...attStatusByStudent };
    for (const s of studentsInClass) next[String(s.id)] = "HOLIDAY";
    setAttStatusByStudent(next);
  };

  const submitAttendance = async (e) => {
    e.preventDefault();
    setAttMsg({ type: "", text: "" });
    if (!attClassId || !attDate) {
      setAttMsg({ type: "err", text: "Select a class and date." });
      return;
    }
    const cid = Number(attClassId);
    if (!Number.isFinite(cid) || cid <= 0) {
      setAttMsg({ type: "err", text: "Select a valid class." });
      return;
    }
    const body = {
      classroomId: cid,
      date: attDate,
      students: studentsInClass.map((s) => {
        const raw = attStatusByStudent[String(s.id)] || "PRESENT";
        return {
          studentId: Number(s.id),
          status: String(raw).toUpperCase(),
        };
      }),
    };
    if (body.students.length === 0) {
      setAttMsg({ type: "err", text: "No students in this class." });
      return;
    }
    try {
      await apiRequest("/api/attendance/bulk", "POST", body);
      setAttMsg({
        type: "ok",
        text: "Attendance saved. Each student was sent an in-app notification.",
      });
    } catch (err) {
      setAttMsg({ type: "err", text: err.message || "Could not save attendance." });
    }
  };

  const submitQuiz = async (e) => {
    e.preventDefault();
    setQuizMsg({ type: "", text: "" });
    if (!quizForm.title?.trim()) {
      setQuizMsg({ type: "err", text: "Title is required." });
      return;
    }
    if (!quizForm.classroomId) {
      setQuizMsg({ type: "err", text: "Select a class first." });
      return;
    }
    if (!quizForm.subjectId) {
      setQuizMsg({ type: "err", text: "Select a subject for this class." });
      return;
    }
    setSavingQuiz(true);
    try {
      const created = await apiRequest("/api/quizzes", "POST", {
        title: quizForm.title.trim(),
        totalMarks: Number(quizForm.totalMarks) || 0,
        durationMinutes: Number(quizForm.durationMinutes) || 0,
        subjectId: Number(quizForm.subjectId),
        classroomId: Number(quizForm.classroomId),
      });
      setQuizMsg({ type: "ok", text: "Exam created. Add questions below." });
      setQuizForm((f) => ({
        ...f,
        title: "",
      }));
      setSelectedQuizId(created?.id != null ? String(created.id) : "");
      await loadCore();
    } catch (err) {
      setQuizMsg({ type: "err", text: err.message || "Could not create exam." });
    } finally {
      setSavingQuiz(false);
    }
  };

  const submitQuestion = async (e) => {
    e.preventDefault();
    setQMsg({ type: "", text: "" });
    const qid = selectedQuizId ? Number(selectedQuizId) : null;
    if (!qid) {
      setQMsg({ type: "err", text: "Select an exam (or create one first)." });
      return;
    }
    const o = questionForm;
    if (!o.question?.trim() || !o.optionA?.trim() || !o.optionB?.trim()) {
      setQMsg({ type: "err", text: "Question and at least options A & B are required." });
      return;
    }
    if (!o.correctAnswer?.trim()) {
      setQMsg({ type: "err", text: "Set the correct answer (must match one option exactly)." });
      return;
    }
    setSavingQ(true);
    try {
      await apiRequest("/api/questions", "POST", {
        quizId: qid,
        question: o.question.trim(),
        optionA: o.optionA.trim(),
        optionB: o.optionB.trim(),
        optionC: (o.optionC || "").trim(),
        optionD: (o.optionD || "").trim(),
        correctAnswer: o.correctAnswer.trim(),
        marks: Number(o.marks) || 1,
      });
      setQMsg({ type: "ok", text: "Question added." });
      setQuestionForm({
        question: "",
        optionA: "",
        optionB: "",
        optionC: "",
        optionD: "",
        correctAnswer: "",
        marks: 1,
      });
    } catch (err) {
      setQMsg({ type: "err", text: err.message || "Could not add question." });
    } finally {
      setSavingQ(false);
    }
  };

  const runImportCsv = async (fileList) => {
    const file = fileList?.[0];
    if (!selectedQuizId) {
      setImportFeedback({ ok: false, text: "Select an exam first.", errors: [] });
      return;
    }
    if (!file) {
      setImportFeedback({ ok: false, text: "Choose a CSV file.", errors: [] });
      return;
    }
    setImportBusy(true);
    setImportFeedback(null);
    try {
      const fd = new FormData();
      fd.append("quizId", selectedQuizId);
      fd.append("file", file);
      const r = await apiUploadMultipart("/institute/quiz-questions/import/csv", fd);
      const skipped = r.failedCount > 0 ? ` ${r.failedCount} row(s) had issues.` : "";
      setImportFeedback({
        ok: true,
        text: `Imported ${r.importedCount} question(s).${skipped}`,
        errors: r.errors || [],
      });
      setImportCsvKey((k) => k + 1);
      await loadCore();
    } catch (err) {
      setImportFeedback({ ok: false, text: err.message || "CSV import failed.", errors: [] });
    } finally {
      setImportBusy(false);
    }
  };

  const runImportPdf = async (fileList) => {
    const file = fileList?.[0];
    if (!selectedQuizId) {
      setImportFeedback({ ok: false, text: "Select an exam first.", errors: [] });
      return;
    }
    if (!file) {
      setImportFeedback({ ok: false, text: "Choose a PDF file.", errors: [] });
      return;
    }
    setImportBusy(true);
    setImportFeedback(null);
    try {
      const fd = new FormData();
      fd.append("quizId", selectedQuizId);
      fd.append("file", file);
      const r = await apiUploadMultipart("/institute/quiz-questions/import/pdf", fd);
      const skipped = r.failedCount > 0 ? ` ${r.failedCount} line(s) had issues.` : "";
      setImportFeedback({
        ok: true,
        text: `Imported ${r.importedCount} question(s) from PDF.${skipped}`,
        errors: r.errors || [],
      });
      setImportPdfKey((k) => k + 1);
      await loadCore();
    } catch (err) {
      setImportFeedback({ ok: false, text: err.message || "PDF import failed.", errors: [] });
    } finally {
      setImportBusy(false);
    }
  };

  const runStudentUpload = async (fileList) => {
    const file = fileList?.[0];
    setStudentUploadMsg({ type: "", text: "" });
    if (!uploadClassId) {
      setStudentUploadMsg({ type: "err", text: "Select your homeroom class first." });
      return;
    }
    if (!file) {
      setStudentUploadMsg({ type: "err", text: "Choose a CSV or Excel file." });
      return;
    }
    if (studentUploadInFlight.current) {
      return;
    }
    const cid = Number(uploadClassId);
    if (!Number.isFinite(cid) || cid <= 0) {
      setStudentUploadMsg({ type: "err", text: "Invalid class selection. Refresh the page and pick your homeroom class again." });
      return;
    }
    studentUploadInFlight.current = true;
    setStudentUploadBusy(true);
    try {
      const fd = new FormData();
      fd.append("file", file);
      fd.append("classroomId", String(cid));
      await apiUploadMultipart("/institute/students/upload", fd);
      setStudentUploadMsg({
        type: "ok",
        text: "Students imported. Each student receives an email with login details and a reminder to change their password after first login.",
      });
      setStudentUploadKey((k) => k + 1);
      await loadCore();
    } catch (err) {
      const m = err?.message || "Import failed.";
      setStudentUploadMsg({ type: "err", text: m });
      console.error("Student import failed:", m);
    } finally {
      studentUploadInFlight.current = false;
      setStudentUploadBusy(false);
    }
  };

  if (loading && !me) {
    return (
      <InstitutePortalLayout
        portalTitle="Teacher"
        portalTagline="Loading…"
        sidebarNav={[{ id: "overview", label: "Overview" }]}
        activeNavId="overview"
        onNavClick={() => {}}
      >
        <div className="p-8 text-slate-600">Loading…</div>
      </InstitutePortalLayout>
    );
  }

  return (
    <InstitutePortalLayout
      portalTitle={portalTitle}
      portalTagline={portalTagline}
      sidebarNav={navItems}
      activeNavId={activeNavId}
      onNavClick={setActiveNavId}
    >
      <div className="p-6 max-w-5xl mx-auto space-y-8">
        <header>
          <h1 className="text-2xl font-bold text-slate-900">{mainDashboardTitle}</h1>
          <p className="text-slate-600 mt-1">
            {me?.name ? (
              <>
                Signed in as <span className="font-medium text-slate-800">{me.name}</span>
                {me.instituteName ? (
                  <span className="text-slate-500"> — {me.instituteName}</span>
                ) : null}
              </>
            ) : (
              "Your classes and teaching tools"
            )}
          </p>
          {teacherMode === "both" && (
            <p className="text-sm text-slate-600 mt-2 max-w-2xl">
              You have <strong>two roles</strong> here: <span className="text-violet-800 font-medium">class teacher</span>{" "}
              (homeroom, roster import) and <span className="text-sky-800 font-medium">subject teacher</span> (subjects &
              exams). Use the matching sidebar sections — they are not the same job.
            </p>
          )}
          {(isHomeroomTeacher || isSubjectTeacher) && (
            <p className="text-sm text-slate-500 mt-2 flex flex-wrap gap-2">
              {isHomeroomTeacher && (
                <span className="inline-flex items-center rounded-full bg-violet-100 text-violet-800 px-2.5 py-0.5 font-medium">
                  Class teacher (homeroom)
                </span>
              )}
              {isSubjectTeacher && (
                <span className="inline-flex items-center rounded-full bg-sky-100 text-sky-800 px-2.5 py-0.5 font-medium">
                  Subject teacher
                </span>
              )}
            </p>
          )}
        </header>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm">{error}</div>
        )}

        {activeNavId === "overview" && (
          <div className="space-y-6">
            <section className="rounded-xl border border-slate-200 bg-slate-50/80 p-4 text-sm text-slate-700">
              {isHomeroomTeacher && !isSubjectTeacher && (
                <p>
                  You are only a <strong>class teacher</strong> here: homeroom class, roster, and class-level attendance.
                  Open <strong>Homeroom & students</strong> to import students (CSV or Excel). You do not have a separate
                  “subject teacher” assignment in this system unless your institute adds one.
                </p>
              )}
              {isSubjectTeacher && !isHomeroomTeacher && (
                <p>
                  You are a <strong>subject teacher</strong> only: you teach assigned subjects and use the timetable.
                  Use <strong>Attendance</strong> and <strong>Exams (your subjects)</strong> for those classes.{" "}
                  <strong>Homeroom & students</strong> and bulk student import are for class teachers only — not shown in
                  your menu.
                </p>
              )}
              {isHomeroomTeacher && isSubjectTeacher && (
                <p>
                  <strong>Class teacher</strong> work (homeroom, import roster) lives under{" "}
                  <strong>Homeroom & students</strong>. <strong>Subject teacher</strong> work (exams for subjects you teach)
                  lives under <strong>Exams</strong>. Keep the two separate — different responsibilities.
                </p>
              )}
              {!isHomeroomTeacher && !isSubjectTeacher && (
                <p>
                  Your dashboard shows the classes and students you are allowed to see (for example from your timetable).
                  Contact your institute if something is missing.
                </p>
              )}
            </section>
            <section className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
                <div className="text-3xl font-bold text-indigo-700">{classrooms.length}</div>
                <div className="text-sm text-slate-600 mt-1">Classes you can access</div>
              </div>
              <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
                <div className="text-3xl font-bold text-indigo-700">
                  {isHomeroomTeacher ? homeroomStudents.length : students.length}
                </div>
                <div className="text-sm text-slate-600 mt-1">
                  {isHomeroomTeacher ? "Students in your homeroom class(es)" : "Students (your classes)"}
                </div>
              </div>
              <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
                <div className="text-3xl font-bold text-indigo-700">{quizzes.length}</div>
                <div className="text-sm text-slate-600 mt-1">Exams you manage</div>
              </div>
            </section>
          </div>
        )}

        {activeNavId === "my-class" && isHomeroomTeacher && (
          <div className="space-y-8 border-l-4 border-violet-500 pl-4 -ml-1">
            <p className="text-sm font-medium text-violet-900">Class teacher — homeroom only</p>
            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
              <h2 className="text-lg font-semibold text-slate-800">Import students</h2>
              <p className="text-sm text-slate-600">
                Upload a UTF-8 <strong>CSV</strong> (header row: name, email, rollNumber) or an <strong>Excel</strong>{" "}
                (.xlsx) with columns: name, email, roll number. New students receive an email with a temporary password
                and must change it after first login.
              </p>
              <details className="text-sm text-slate-600">
                <summary className="cursor-pointer text-indigo-700 font-medium">Example CSV</summary>
                <pre className="mt-2 p-3 bg-slate-50 rounded-lg text-xs overflow-x-auto border border-slate-100">
                  {`name,email,rollNumber
Ada Lovelace,ada@example.com,101
Alan Turing,alan@example.com,102`}
                </pre>
              </details>
              <div className="flex flex-wrap gap-4 items-end">
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Homeroom class</span>
                  <select
                    className="border border-slate-300 rounded-lg px-3 py-2 min-w-[220px] bg-white"
                    value={uploadClassId}
                    onChange={(e) => setUploadClassId(e.target.value)}
                  >
                    {homeroomClassrooms.map((c) => (
                      <option key={c.id} value={String(c.id)}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">CSV or Excel</span>
                  <input
                    key={studentUploadKey}
                    type="file"
                    accept=".csv,.xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/csv"
                    disabled={studentUploadBusy || homeroomClassrooms.length === 0}
                    className="text-sm"
                    onChange={(e) => {
                      const files = e.target.files;
                      if (files?.length) runStudentUpload(files);
                      e.target.value = "";
                    }}
                  />
                </label>
              </div>
              {studentUploadBusy && <p className="text-sm text-slate-500">Uploading…</p>}
              {studentUploadMsg.text && (
                <div
                  className={`text-sm px-3 py-2 rounded-lg border ${
                    studentUploadMsg.type === "ok"
                      ? "bg-emerald-50 text-emerald-900 border-emerald-200"
                      : "bg-red-50 text-red-800 border-red-200"
                  }`}
                >
                  {studentUploadMsg.text}
                </div>
              )}
            </section>

            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-3">Students in your homeroom class(es)</h2>
              {homeroomStudents.length === 0 ? (
                <p className="text-sm text-slate-500">No students yet. Import a list above or ask your admin.</p>
              ) : (
                <ul className="divide-y divide-slate-100 max-h-[420px] overflow-y-auto">
                  {homeroomStudents.map((s) => (
                    <li key={s.id} className="py-3 flex flex-wrap justify-between gap-2">
                      <div>
                        <div className="font-medium text-slate-900">{s.name}</div>
                        <div className="text-sm text-slate-600">
                          {s.email}
                          {s.rollNumber ? ` · Roll ${s.rollNumber}` : ""}
                          {s.classroomName ? ` · ${s.classroomName}` : ""}
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        )}

        {activeNavId === "attendance" && (
          <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-6">
            <div>
              <h2 className="text-lg font-semibold text-slate-800">Mark attendance</h2>
              <p className="text-sm text-slate-600 mt-1">
                {teacherMode === "subject_only" && (
                  <>
                    As a <strong>subject teacher</strong>, pick a class where you are on the timetable or assigned for your
                    subject. You are not the homeroom teacher unless your institute also assigns you as class teacher.
                    Use <strong>Holiday</strong> when there is no class (not counted as absent).
                  </>
                )}
                {teacherMode === "class_only" && (
                  <>
                    As <strong>class teacher</strong>, you mark attendance for your homeroom class(es) and any class period
                    where you appear on the schedule.
                  </>
                )}
                {(teacherMode === "both" || teacherMode === "other") && (
                  <>
                    Choose a class and date. You may mark attendance as class teacher, or when you have a timetable slot
                    for that class. Use <strong>Holiday</strong> when class is off (holiday does not count as absent in
                    attendance %).
                  </>
                )}
              </p>
            </div>

            <form onSubmit={submitAttendance} className="space-y-4">
              <div className="flex flex-wrap gap-4 items-end">
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Class</span>
                  <select
                    className="border border-slate-300 rounded-lg px-3 py-2 min-w-[200px] bg-white"
                    value={attClassId}
                    onChange={(e) => setAttClassId(e.target.value)}
                  >
                    <option value="">Select class</option>
                    {classrooms.map((c) => (
                      <option key={c.id} value={String(c.id)}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Date</span>
                  <input
                    type="date"
                    className="border border-slate-300 rounded-lg px-3 py-2"
                    value={attDate}
                    onChange={(e) => setAttDate(e.target.value)}
                  />
                  <span className="text-xs text-slate-500">Any calendar day, including weekends.</span>
                </label>
              </div>

              {attMsg.text && (
                <div
                  className={`text-sm px-3 py-2 rounded-lg ${
                    attMsg.type === "ok" ? "bg-emerald-50 text-emerald-800 border border-emerald-200" : "bg-red-50 text-red-800 border border-red-200"
                  }`}
                >
                  {attMsg.text}
                </div>
              )}

              {attClassId && (
                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <div className="bg-slate-50 px-4 py-2 flex flex-wrap items-center justify-between gap-2 text-sm font-medium text-slate-700">
                    <span>
                      Students {attLoading && <span className="text-slate-500 font-normal">(loading…)</span>}
                    </span>
                    {studentsInClass.length > 0 && (
                      <button
                        type="button"
                        onClick={markAllHoliday}
                        className="text-xs font-medium px-2.5 py-1 rounded-md bg-amber-100 text-amber-900 hover:bg-amber-200 border border-amber-200"
                      >
                        Mark all as holiday
                      </button>
                    )}
                  </div>
                  {studentsInClass.length === 0 ? (
                    <div className="p-4 text-sm text-slate-500">No students in this class.</div>
                  ) : (
                    <ul className="divide-y divide-slate-100">
                      {studentsInClass.map((s) => {
                        const st = attStatusByStudent[String(s.id)] || "PRESENT";
                        return (
                          <li key={s.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
                            <div>
                              <div className="font-medium text-slate-900">{s.name}</div>
                              <div className="text-xs text-slate-500">
                                {s.rollNumber ? `Roll ${s.rollNumber}` : ""}
                                {s.classroomName ? ` · ${s.classroomName}` : ""}
                              </div>
                            </div>
                            <div className="flex flex-wrap gap-2">
                              <button
                                type="button"
                                onClick={() => setStatus(s.id, "PRESENT")}
                                className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
                                  st === "PRESENT"
                                    ? "bg-emerald-600 text-white"
                                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                                }`}
                              >
                                Present
                              </button>
                              <button
                                type="button"
                                onClick={() => setStatus(s.id, "ABSENT")}
                                className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
                                  st === "ABSENT"
                                    ? "bg-rose-600 text-white"
                                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                                }`}
                              >
                                Absent
                              </button>
                              <button
                                type="button"
                                onClick={() => setStatus(s.id, "HOLIDAY")}
                                className={`px-3 py-1.5 rounded-lg text-sm font-medium ${
                                  st === "HOLIDAY"
                                    ? "bg-amber-500 text-white"
                                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                                }`}
                              >
                                Holiday
                              </button>
                            </div>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </div>
              )}

              {attClassId && studentsInClass.length > 0 && (
                <button
                  type="submit"
                  className="px-4 py-2.5 rounded-lg bg-indigo-600 text-white font-medium hover:bg-indigo-700"
                >
                  Save attendance
                </button>
              )}
            </form>
          </section>
        )}

        {activeNavId === "exams" && (
          <div
            className={`space-y-8 ${
              teacherMode === "subject_only" ? "border-l-4 border-sky-500 pl-4 -ml-1" : ""
            }`}
          >
            {teacherMode === "subject_only" && (
              <p className="text-sm font-medium text-sky-900">Subject teacher — exams for subjects you teach</p>
            )}
            {teacherMode === "class_only" && (
              <p className="text-sm font-medium text-violet-900">
                Class teacher — exams for classes where you are allowed (e.g. your homeroom)
              </p>
            )}
            {teacherMode === "both" && (
              <p className="text-sm text-slate-700 max-w-3xl">
                <span className="font-medium text-violet-800">Class teacher:</span> you may create exams tied to your
                homeroom classes. <span className="font-medium text-sky-800">Subject teacher:</span> pick subjects you
                teach — not the same as roster / import work in Homeroom & students.
              </p>
            )}
            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
              <h2 className="text-lg font-semibold text-slate-800">Create exam</h2>
              <p className="text-sm text-slate-600">
                {teacherMode === "subject_only" && (
                  <>
                    Choose a <strong>subject you teach</strong> and a <strong>class</strong> you are assigned to. This is
                    subject-teacher work, not homeroom roster management.
                  </>
                )}
                {teacherMode === "class_only" && (
                  <>
                    Create a quiz for a subject and one of <strong>your classes</strong> (for example your homeroom), per
                    institute rules.
                  </>
                )}
                {(teacherMode === "both" || teacherMode === "other") && (
                  <>
                    Create a quiz for a subject and class you are allowed to manage: as class teacher for that class, or as
                    subject teacher for that subject.
                  </>
                )}
              </p>
              <form onSubmit={submitQuiz} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm sm:col-span-2">
                  <span className="text-slate-700 font-medium">Title</span>
                  <input
                    className="border border-slate-300 rounded-lg px-3 py-2"
                    value={quizForm.title}
                    onChange={(e) => setQuizForm((f) => ({ ...f, title: e.target.value }))}
                    placeholder="e.g. Unit 1 test"
                  />
                </label>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Class</span>
                  <select
                    className="border border-slate-300 rounded-lg px-3 py-2 bg-white"
                    value={quizForm.classroomId}
                    onChange={(e) =>
                      setQuizForm((f) => ({ ...f, classroomId: e.target.value, subjectId: "" }))
                    }
                  >
                    <option value="">Select class</option>
                    {classrooms.map((c) => (
                      <option key={c.id} value={String(c.id)}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Subject</span>
                  <select
                    className="border border-slate-300 rounded-lg px-3 py-2 bg-white disabled:bg-slate-100 disabled:text-slate-500"
                    disabled={!quizForm.classroomId || quizSubjectsLoading}
                    value={quizForm.subjectId}
                    onChange={(e) => setQuizForm((f) => ({ ...f, subjectId: e.target.value }))}
                  >
                    <option value="">
                      {!quizForm.classroomId
                        ? "Select a class first"
                        : quizSubjectsLoading
                          ? "Loading subjects…"
                          : "Select subject"}
                    </option>
                    {quizSubjects.map((s) => (
                      <option key={s.id} value={String(s.id)}>
                        {s.name} {s.code ? `(${s.code})` : ""}
                      </option>
                    ))}
                  </select>
                  {quizForm.classroomId && !quizSubjectsLoading && quizSubjects.length === 0 && (
                    <span className="text-xs text-amber-700 mt-1">
                      No subjects for this class in the timetable. Add schedule slots (class + subject) in admin, then try
                      again.
                    </span>
                  )}
                </label>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Total marks</span>
                  <input
                    type="number"
                    min={1}
                    className="border border-slate-300 rounded-lg px-3 py-2"
                    value={quizForm.totalMarks}
                    onChange={(e) => setQuizForm((f) => ({ ...f, totalMarks: e.target.value }))}
                  />
                </label>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Duration (minutes)</span>
                  <input
                    type="number"
                    min={1}
                    className="border border-slate-300 rounded-lg px-3 py-2"
                    value={quizForm.durationMinutes}
                    onChange={(e) => setQuizForm((f) => ({ ...f, durationMinutes: e.target.value }))}
                  />
                </label>
                {quizMsg.text && (
                  <div
                    className={`sm:col-span-2 text-sm px-3 py-2 rounded-lg ${
                      quizMsg.type === "ok"
                        ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                        : "bg-red-50 text-red-800 border border-red-200"
                    }`}
                  >
                    {quizMsg.text}
                  </div>
                )}
                <div className="sm:col-span-2">
                  <button
                    type="submit"
                    disabled={savingQuiz}
                    className="px-4 py-2.5 rounded-lg bg-indigo-600 text-white font-medium hover:bg-indigo-700 disabled:opacity-60"
                  >
                    {savingQuiz ? "Creating…" : "Create exam"}
                  </button>
                </div>
              </form>
            </section>

            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
              <h2 className="text-lg font-semibold text-slate-800">Add questions</h2>
              <p className="text-sm text-slate-600">
                Correct answer must match one of the four options exactly (case-insensitive when students take the quiz).
              </p>
              <label className="flex flex-col gap-1 text-sm max-w-md">
                <span className="text-slate-700 font-medium">Exam</span>
                <select
                  className="border border-slate-300 rounded-lg px-3 py-2 bg-white"
                  value={selectedQuizId}
                  onChange={(e) => setSelectedQuizId(e.target.value)}
                >
                  <option value="">Select exam</option>
                  {quizzes.map((q) => (
                    <option key={q.id} value={String(q.id)}>
                      {q.title} — {q.subjectName} / {q.classroomName}
                    </option>
                  ))}
                </select>
              </label>
              <form onSubmit={submitQuestion} className="space-y-3 max-w-2xl">
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Question</span>
                  <textarea
                    className="border border-slate-300 rounded-lg px-3 py-2 min-h-[80px]"
                    value={questionForm.question}
                    onChange={(e) => setQuestionForm((f) => ({ ...f, question: e.target.value }))}
                  />
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {["optionA", "optionB", "optionC", "optionD"].map((key) => (
                    <label key={key} className="flex flex-col gap-1 text-sm">
                      <span className="text-slate-700 font-medium">{key.replace("option", "Option ")}</span>
                      <input
                        className="border border-slate-300 rounded-lg px-3 py-2"
                        value={questionForm[key]}
                        onChange={(e) => setQuestionForm((f) => ({ ...f, [key]: e.target.value }))}
                      />
                    </label>
                  ))}
                </div>
                <label className="flex flex-col gap-1 text-sm">
                  <span className="text-slate-700 font-medium">Correct answer (exact text of one option)</span>
                  <input
                    className="border border-slate-300 rounded-lg px-3 py-2"
                    value={questionForm.correctAnswer}
                    onChange={(e) => setQuestionForm((f) => ({ ...f, correctAnswer: e.target.value }))}
                  />
                </label>
                <label className="flex flex-col gap-1 text-sm max-w-xs">
                  <span className="text-slate-700 font-medium">Marks</span>
                  <input
                    type="number"
                    min={1}
                    className="border border-slate-300 rounded-lg px-3 py-2"
                    value={questionForm.marks}
                    onChange={(e) => setQuestionForm((f) => ({ ...f, marks: e.target.value }))}
                  />
                </label>
                {qMsg.text && (
                  <div
                    className={`text-sm px-3 py-2 rounded-lg ${
                      qMsg.type === "ok"
                        ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                        : "bg-red-50 text-red-800 border border-red-200"
                    }`}
                  >
                    {qMsg.text}
                  </div>
                )}
                <button
                  type="submit"
                  disabled={savingQ}
                  className="px-4 py-2.5 rounded-lg bg-slate-800 text-white font-medium hover:bg-slate-900 disabled:opacity-60"
                >
                  {savingQ ? "Saving…" : "Add question"}
                </button>
              </form>

              <div className="mt-8 pt-8 border-t border-slate-200 space-y-4">
                <h3 className="text-md font-semibold text-slate-800">Bulk import (CSV or PDF)</h3>
                <p className="text-sm text-slate-600">
                  Use the same exam as above. CSV must include a header row. PDF import reads plain text: one question per
                  line, same columns as CSV without a header (comma-separated, quote fields that contain commas).
                </p>
                <details className="text-sm text-slate-600">
                  <summary className="cursor-pointer text-indigo-700 font-medium">Example CSV</summary>
                  <pre className="mt-2 p-3 bg-slate-50 rounded-lg text-xs overflow-x-auto border border-slate-100">
                    {`question,optionA,optionB,optionC,optionD,correctAnswer,marks
"What is 2+2?","1","2","3","4","4",1
Capital of France?,"Berlin","Madrid","Paris","Rome",C,1`}
                  </pre>
                </details>
                <div className="flex flex-wrap gap-4 items-end">
                  <label className="flex flex-col gap-1 text-sm">
                    <span className="text-slate-700 font-medium">CSV file</span>
                    <input
                      key={importCsvKey}
                      type="file"
                      accept=".csv,text/csv"
                      disabled={importBusy}
                      className="text-sm"
                      onChange={(e) => {
                        const files = e.target.files;
                        if (files?.length) runImportCsv(files);
                        e.target.value = "";
                      }}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-sm">
                    <span className="text-slate-700 font-medium">PDF file</span>
                    <input
                      key={importPdfKey}
                      type="file"
                      accept=".pdf,application/pdf"
                      disabled={importBusy}
                      className="text-sm"
                      onChange={(e) => {
                        const files = e.target.files;
                        if (files?.length) runImportPdf(files);
                        e.target.value = "";
                      }}
                    />
                  </label>
                </div>
                {importBusy && <p className="text-sm text-slate-500">Importing…</p>}
                {importFeedback && (
                  <div
                    className={`text-sm px-3 py-2 rounded-lg border ${
                      importFeedback.ok
                        ? "bg-emerald-50 text-emerald-900 border-emerald-200"
                        : "bg-red-50 text-red-800 border-red-200"
                    }`}
                  >
                    <div>{importFeedback.text}</div>
                    {importFeedback.errors?.length > 0 && (
                      <ul className="mt-2 list-disc list-inside text-xs space-y-0.5 max-h-40 overflow-y-auto">
                        {importFeedback.errors.map((line, i) => (
                          <li key={i}>{line}</li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
              </div>
            </section>

            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-3">Your exams</h2>
              {quizzes.length === 0 ? (
                <p className="text-sm text-slate-500">No exams yet. Create one above.</p>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {quizzes.map((q) => (
                    <li key={q.id} className="py-3 flex flex-wrap justify-between gap-2">
                      <div>
                        <div className="font-medium text-slate-900">{q.title}</div>
                        <div className="text-sm text-slate-600">
                          {q.subjectName} · {q.classroomName} · {q.totalMarks} marks · {q.durationMinutes} min
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        )}
      </div>
    </InstitutePortalLayout>
  );
}

export default TeacherDashboard;
