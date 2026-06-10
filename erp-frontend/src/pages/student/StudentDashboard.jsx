import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import InstitutePortalLayout from "../../components/InstitutePortalLayout";
import { apiRequest } from "../../services/Api";

const NAV = [
  { id: "overview", label: "Overview" },
  { id: "notifications", label: "Notifications" },
  { id: "attendance", label: "Attendance" },
  { id: "fees", label: "Fees" },
  { id: "exams", label: "Exams" },
];

function AttendancePieChart({ present = 0, absent = 0, holiday = 0 }) {
  const total = present + absent + holiday;
  const radius = 72;
  const circumference = 2 * Math.PI * radius;
  const pctPresent = total > 0 ? present / total : 0;
  const pctAbsent = total > 0 ? absent / total : 0;
  const pctHoliday = total > 0 ? holiday / total : 0;

  const presentLen = circumference * pctPresent;
  const absentLen = circumference * pctAbsent;
  const holidayLen = circumference * pctHoliday;

  return (
    <div className="flex flex-wrap items-center gap-6">
      <div className="relative w-[180px] h-[180px]">
        <svg width="180" height="180" viewBox="0 0 180 180" className="-rotate-90">
          <circle cx="90" cy="90" r={radius} fill="none" stroke="#e2e8f0" strokeWidth="20" />
          <circle
            cx="90"
            cy="90"
            r={radius}
            fill="none"
            stroke="#059669"
            strokeWidth="20"
            strokeDasharray={`${presentLen} ${circumference - presentLen}`}
            strokeDashoffset="0"
            strokeLinecap="butt"
          />
          <circle
            cx="90"
            cy="90"
            r={radius}
            fill="none"
            stroke="#be123c"
            strokeWidth="20"
            strokeDasharray={`${absentLen} ${circumference - absentLen}`}
            strokeDashoffset={-presentLen}
            strokeLinecap="butt"
          />
          <circle
            cx="90"
            cy="90"
            r={radius}
            fill="none"
            stroke="#d97706"
            strokeWidth="20"
            strokeDasharray={`${holidayLen} ${circumference - holidayLen}`}
            strokeDashoffset={-(presentLen + absentLen)}
            strokeLinecap="butt"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <div className="text-2xl font-bold text-slate-800">{total}</div>
          <div className="text-xs text-slate-500">Total days</div>
        </div>
      </div>

      <div className="text-sm space-y-2">
        <div className="flex items-center gap-2 text-slate-700">
          <span className="inline-block w-3 h-3 rounded-sm bg-emerald-600" />
          Present: <span className="font-semibold">{present}</span>
        </div>
        <div className="flex items-center gap-2 text-slate-700">
          <span className="inline-block w-3 h-3 rounded-sm bg-rose-700" />
          Absent: <span className="font-semibold">{absent}</span>
        </div>
        <div className="flex items-center gap-2 text-slate-700">
          <span className="inline-block w-3 h-3 rounded-sm bg-amber-600" />
          Holiday: <span className="font-semibold">{holiday}</span>
        </div>
      </div>
    </div>
  );
}

function StudentDashboard() {
  const [activeNavId, setActiveNavId] = useState("overview");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [me, setMe] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [attendanceRows, setAttendanceRows] = useState([]);
  const [attendanceReport, setAttendanceReport] = useState(null);
  const [fees, setFees] = useState([]);
  const [expandedFeeHistory, setExpandedFeeHistory] = useState({});
  const [feeHistoryByFeeId, setFeeHistoryByFeeId] = useState({});
  const [feeHistoryErrorByFeeId, setFeeHistoryErrorByFeeId] = useState({});
  const [feeHistoryLoadingByFeeId, setFeeHistoryLoadingByFeeId] = useState({});
  const [quizzes, setQuizzes] = useState([]);
  const [quizResults, setQuizResults] = useState([]);

  const [selectedQuizId, setSelectedQuizId] = useState("");
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [quizLoading, setQuizLoading] = useState(false);
  const [submitMsg, setSubmitMsg] = useState({ type: "", text: "" });
  const [lastResult, setLastResult] = useState(null);

  const loadCore = useCallback(async () => {
    setError("");
    setLoading(true);
    try {
      const m = await apiRequest("/api/students/me");
      setMe(m);
      const [notif, att, rep, feeRows, qz, res] = await Promise.all([
        apiRequest("/api/notifications"),
        apiRequest("/api/attendance/my"),
        m?.id != null ? apiRequest(`/api/attendance/report/${m.id}`) : Promise.resolve(null),
        apiRequest("/api/fees/my"),
        apiRequest("/api/quizzes"),
        m?.id != null ? apiRequest(`/api/quizzes/results/${m.id}`) : Promise.resolve([]),
      ]);
      setNotifications(Array.isArray(notif) ? notif : []);
      setAttendanceRows(Array.isArray(att) ? att : []);
      setAttendanceReport(rep);
      setFees(Array.isArray(feeRows) ? feeRows : []);
      setExpandedFeeHistory({});
      setFeeHistoryByFeeId({});
      setFeeHistoryErrorByFeeId({});
      setFeeHistoryLoadingByFeeId({});
      setQuizzes(Array.isArray(qz) ? qz : []);
      setQuizResults(Array.isArray(res) ? res : []);
    } catch (e) {
      setError(e.message || "Failed to load.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCore();
  }, [loadCore]);

  const unreadCount = useMemo(
    () => notifications.filter((n) => n && !n.read).length,
    [notifications]
  );

  const attendanceChartData = useMemo(() => {
    if (attendanceReport) {
      return {
        present: Number(attendanceReport.presentDays || 0),
        absent: Number(attendanceReport.absentDays || 0),
        holiday: Number(attendanceReport.holidayDays || 0),
      };
    }
    let present = 0;
    let absent = 0;
    let holiday = 0;
    for (const row of attendanceRows) {
      if (row?.status === "PRESENT") present += 1;
      else if (row?.status === "ABSENT") absent += 1;
      else if (row?.status === "HOLIDAY") holiday += 1;
    }
    return { present, absent, holiday };
  }, [attendanceReport, attendanceRows]);

  const feeSummary = useMemo(() => {
    let unpaid = 0;
    let paid = 0;
    for (const f of fees) {
      const amount = Number(f?.amount || 0);
      if (f?.status === "PAID") paid += amount;
      else unpaid += amount;
    }
    return { paid, unpaid };
  }, [fees]);

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
      const rows = await apiRequest(`/api/fees/my/${feeId}/payments`);
      setFeeHistoryByFeeId((s) => ({ ...s, [feeId]: Array.isArray(rows) ? rows : [] }));
    } catch (err) {
      setFeeHistoryErrorByFeeId((s) => ({ ...s, [feeId]: err.message || "Could not load payment history." }));
    } finally {
      setFeeHistoryLoadingByFeeId((s) => ({ ...s, [feeId]: false }));
    }
  };

  const payOnlineMock = async (fee) => {
    const amountRaw = window.prompt("Enter online payment amount:", String(fee.balanceAmount ?? ""));
    if (!amountRaw) return;
    const amount = Number(amountRaw);
    if (!Number.isFinite(amount) || amount <= 0) {
      alert("Enter a valid amount.");
      return;
    }
    try {
      const init = await apiRequest(`/api/fees/my/${fee.id}/online/init`, "POST", {
        amount,
        gateway: "MOCKPAY",
      });
      await apiRequest(`/api/fees/my/${fee.id}/online/confirm`, "POST", {
        orderId: init?.orderId,
        paymentId: `PAY-${Date.now()}`,
        signature: "MOCK-SIGNATURE",
        status: "SUCCESS",
        rawPayload: JSON.stringify({ mock: true, orderId: init?.orderId }),
      });
      alert("Online payment successful.");
      await loadCore();
    } catch (err) {
      alert(err?.message || "Online payment failed.");
    }
  };

  const loadQuestions = async (quizId) => {
    if (!quizId) {
      setQuestions([]);
      return;
    }
    setQuizLoading(true);
    setSubmitMsg({ type: "", text: "" });
    setLastResult(null);
    try {
      const qs = await apiRequest(`/api/quizzes/${quizId}/questions`);
      setQuestions(Array.isArray(qs) ? qs : []);
      setAnswers({});
    } catch (e) {
      setSubmitMsg({ type: "err", text: e.message || "Could not load questions." });
      setQuestions([]);
    } finally {
      setQuizLoading(false);
    }
  };

  useEffect(() => {
    if (selectedQuizId) loadQuestions(selectedQuizId);
  }, [selectedQuizId]);

  const submitQuiz = async (e) => {
    e.preventDefault();
    setSubmitMsg({ type: "", text: "" });
    const qid = selectedQuizId ? Number(selectedQuizId) : null;
    if (!qid) {
      setSubmitMsg({ type: "err", text: "Select an exam." });
      return;
    }
    const answerList = questions.map((q) => ({
      questionId: q.id,
      selectedAnswer: (answers[String(q.id)] || "").trim(),
    }));
    if (answerList.some((a) => !a.selectedAnswer)) {
      setSubmitMsg({ type: "err", text: "Answer every question." });
      return;
    }
    try {
      const result = await apiRequest(`/api/quizzes/${qid}/submit`, "POST", {
        studentId: null,
        answers: answerList,
      });
      setLastResult(result);
      setSubmitMsg({ type: "ok", text: "Exam submitted." });
      await loadCore();
    } catch (err) {
      setSubmitMsg({ type: "err", text: err.message || "Submit failed." });
    }
  };

  const markNotificationRead = async (id) => {
    try {
      await apiRequest(`/api/notifications/${id}/read`, "PATCH");
      await loadCore();
    } catch {
      /* ignore */
    }
  };

  if (loading && !me) {
    return (
      <InstitutePortalLayout
        portalTitle="My learning"
        portalTagline="Attendance, exams & notifications"
        sidebarNav={NAV}
        activeNavId={activeNavId}
        onNavClick={setActiveNavId}
      >
        <div className="p-8 text-slate-600">Loading…</div>
      </InstitutePortalLayout>
    );
  }

  return (
    <InstitutePortalLayout
      portalTitle="My learning"
      portalTagline="Attendance, exams & notifications"
      sidebarNav={NAV}
      activeNavId={activeNavId}
      onNavClick={setActiveNavId}
    >
      <div className="p-6 max-w-4xl mx-auto space-y-8">
        <header>
          <h1 className="text-2xl font-bold text-slate-900">My learning</h1>
          <p className="text-slate-600 mt-1">
            {me?.name ? (
              <>
                <span className="font-medium text-slate-800">{me.name}</span>
                {me.classroomName ? <span className="text-slate-500"> — {me.classroomName}</span> : null}
                {me.instituteName ? <span className="text-slate-500"> · {me.instituteName}</span> : null}
              </>
            ) : (
              "Welcome"
            )}
          </p>
        </header>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm">{error}</div>
        )}

        {activeNavId === "overview" && (
          <section className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
              <div className="text-3xl font-bold text-indigo-700">{attendanceRows.length}</div>
              <div className="text-sm text-slate-600 mt-1">Attendance records</div>
            </div>
            <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
              <div className="text-3xl font-bold text-indigo-700">{quizzes.length}</div>
              <div className="text-sm text-slate-600 mt-1">Exams available</div>
            </div>
            <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
              <div className="text-3xl font-bold text-amber-600">{unreadCount}</div>
              <div className="text-sm text-slate-600 mt-1">Unread notifications</div>
            </div>
            <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm sm:col-span-3">
              <div className="text-sm text-slate-600 mb-2">Fee summary</div>
              <div className="flex flex-wrap gap-6">
                <div className="text-rose-700 font-semibold">Pending: Rs. {feeSummary.unpaid.toFixed(2)}</div>
                <div className="text-emerald-700 font-semibold">Paid: Rs. {feeSummary.paid.toFixed(2)}</div>
              </div>
            </div>
          </section>
        )}

        {activeNavId === "notifications" && (
          <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Notifications</h2>
            <p className="text-sm text-slate-600 mb-4">
              When your teacher saves attendance, you get a message here (same as the Attendance tab).
            </p>
            {notifications.length === 0 ? (
              <p className="text-sm text-slate-500">No notifications yet.</p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {notifications.map((n) => (
                  <li key={n.id} className="py-3 flex flex-wrap justify-between gap-2 items-start">
                    <div>
                      <p className={`text-sm ${n.read ? "text-slate-600" : "text-slate-900 font-medium"}`}>{n.message}</p>
                      <p className="text-xs text-slate-400 mt-1">
                        {n.createdAt ? new Date(n.createdAt).toLocaleString() : ""}
                      </p>
                    </div>
                    {!n.read && (
                      <button
                        type="button"
                        onClick={() => markNotificationRead(n.id)}
                        className="text-sm text-indigo-600 hover:underline"
                      >
                        Mark read
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}

        {activeNavId === "attendance" && (
          <div className="space-y-6">
            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Attendance pie chart</h2>
              <AttendancePieChart
                present={attendanceChartData.present}
                absent={attendanceChartData.absent}
                holiday={attendanceChartData.holiday}
              />
            </section>
            {attendanceReport && (
              <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
                <h2 className="text-lg font-semibold text-slate-800 mb-2">Summary</h2>
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 text-sm">
                  <div>
                    <div className="text-slate-500">Present days</div>
                    <div className="text-xl font-semibold text-emerald-700">{attendanceReport.presentDays ?? "—"}</div>
                  </div>
                  <div>
                    <div className="text-slate-500">Absent days</div>
                    <div className="text-xl font-semibold text-rose-700">{attendanceReport.absentDays ?? "—"}</div>
                  </div>
                  <div>
                    <div className="text-slate-500">Holiday</div>
                    <div className="text-xl font-semibold text-amber-700">{attendanceReport.holidayDays ?? "—"}</div>
                  </div>
                  <div>
                    <div className="text-slate-500">Total marked</div>
                    <div className="text-xl font-semibold text-slate-800">{attendanceReport.totalClasses ?? "—"}</div>
                  </div>
                  <div>
                    <div className="text-slate-500">Attendance %</div>
                    <div className="text-xl font-semibold text-indigo-700">
                      {attendanceReport.percentage != null ? `${attendanceReport.percentage.toFixed(1)}%` : "—"}
                    </div>
                  </div>
                </div>
                <p className="text-xs text-slate-500 mt-3">
                  Percentage uses present vs absent only; holiday days are excluded from that calculation.
                </p>
              </section>
            )}
            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Your attendance log</h2>
              {attendanceRows.length === 0 ? (
                <p className="text-sm text-slate-500">No attendance recorded yet.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-left text-slate-600">
                        <th className="py-2 pr-4">Date</th>
                        <th className="py-2 pr-4">Class</th>
                        <th className="py-2">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {attendanceRows.map((r) => (
                        <tr key={r.id} className="border-b border-slate-100">
                          <td className="py-2 pr-4">{r.date}</td>
                          <td className="py-2 pr-4">{r.classroomName || "—"}</td>
                          <td className="py-2">
                            <span
                              className={
                                r.status === "PRESENT"
                                  ? "text-emerald-700 font-medium"
                                  : r.status === "ABSENT"
                                    ? "text-rose-700 font-medium"
                                    : r.status === "HOLIDAY"
                                      ? "text-amber-700 font-medium"
                                      : ""
                              }
                            >
                              {r.status === "HOLIDAY" ? "Holiday" : r.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>
        )}

        {activeNavId === "fees" && (
          <div className="space-y-6">
            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-3">Your fees</h2>
              <div className="flex flex-wrap gap-6 text-sm mb-4 items-center">
                <div className="text-rose-700 font-semibold">Pending: Rs. {feeSummary.unpaid.toFixed(2)}</div>
                <div className="text-emerald-700 font-semibold">Paid: Rs. {feeSummary.paid.toFixed(2)}</div>
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      await downloadFromApi("/api/fees/my/export", "my-fees.csv");
                    } catch (e) {
                      alert(e?.message || "Could not export fees.");
                    }
                  }}
                  className="text-indigo-600 hover:underline text-sm"
                >
                  Export CSV
                </button>
              </div>
              {fees.length === 0 ? (
                <p className="text-sm text-slate-500">No fee records yet.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-left text-slate-600">
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
                            <td className="py-2 pr-4">{f.title}</td>
                            <td className="py-2 pr-4">{f.dueDate || "—"}</td>
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
                                {f.status === "PAID" ? (
                                  <button
                                    type="button"
                                    onClick={async () => {
                                      try {
                                        await downloadFromApi(`/api/fees/my/${f.id}/receipt`, `fee-receipt-${f.id}.pdf`);
                                      } catch (e) {
                                        alert(e?.message || "Could not download receipt.");
                                      }
                                    }}
                                    className="text-indigo-600 hover:underline text-xs"
                                  >
                                    Download receipt
                                  </button>
                                ) : (
                                  <button
                                    type="button"
                                    onClick={() => payOnlineMock(f)}
                                    className="text-indigo-600 hover:underline text-xs"
                                  >
                                    Pay online
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
                              <td colSpan={9} className="px-3 py-3">
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
                </div>
              )}
            </section>
          </div>
        )}

        {activeNavId === "exams" && (
          <div className="space-y-8">
            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
              <h2 className="text-lg font-semibold text-slate-800">Take an exam</h2>
              <p className="text-sm text-slate-600">Choose an exam for your class, answer all questions, then submit.</p>
              <label className="flex flex-col gap-1 text-sm max-w-md">
                <span className="text-slate-700 font-medium">Exam</span>
                <select
                  className="border border-slate-300 rounded-lg px-3 py-2 bg-white"
                  value={selectedQuizId}
                  onChange={(e) => {
                    setSelectedQuizId(e.target.value);
                    setLastResult(null);
                  }}
                >
                  <option value="">Select exam</option>
                  {quizzes.map((q) => (
                    <option key={q.id} value={String(q.id)}>
                      {q.title} — {q.subjectName} ({q.classroomName})
                    </option>
                  ))}
                </select>
              </label>

              {quizLoading && <p className="text-sm text-slate-500">Loading questions…</p>}

              {selectedQuizId && questions.length > 0 && (
                <form onSubmit={submitQuiz} className="space-y-6 mt-4">
                  {questions.map((q, idx) => (
                    <div key={q.id} className="border border-slate-100 rounded-lg p-4 bg-slate-50/50">
                      <div className="font-medium text-slate-900 mb-2">
                        {idx + 1}. {q.question}{" "}
                        <span className="text-slate-500 font-normal">({q.marks} marks)</span>
                      </div>
                      <div className="space-y-2">
                        {(q.options || []).map((opt) => (
                          <label key={opt} className="flex items-center gap-2 text-sm cursor-pointer">
                            <input
                              type="radio"
                              name={`q-${q.id}`}
                              value={opt}
                              checked={answers[String(q.id)] === opt}
                              onChange={() => setAnswers((prev) => ({ ...prev, [String(q.id)]: opt }))}
                            />
                            <span>{opt}</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  ))}
                  {submitMsg.text && (
                    <div
                      className={`text-sm px-3 py-2 rounded-lg ${
                        submitMsg.type === "ok"
                          ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                          : "bg-red-50 text-red-800 border border-red-200"
                      }`}
                    >
                      {submitMsg.text}
                    </div>
                  )}
                  {lastResult && (
                    <div className="text-sm bg-indigo-50 border border-indigo-200 rounded-lg px-3 py-2 text-indigo-900">
                      Score: {lastResult.score} / {lastResult.totalMarks} ({lastResult.percentage?.toFixed?.(1) ?? "—"}%)
                    </div>
                  )}
                  <button
                    type="submit"
                    className="px-4 py-2.5 rounded-lg bg-indigo-600 text-white font-medium hover:bg-indigo-700"
                  >
                    Submit answers
                  </button>
                </form>
              )}
            </section>

            <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-3">Past results</h2>
              {quizResults.length === 0 ? (
                <p className="text-sm text-slate-500">No submitted exams yet.</p>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {quizResults.map((r, i) => (
                    <li key={`${r.quizId}-${i}`} className="py-2 text-sm flex justify-between gap-2">
                      <span className="text-slate-600">Quiz #{r.quizId}</span>
                      <span className="font-medium text-slate-900">
                        {r.score}/{r.totalMarks} ({r.percentage?.toFixed?.(1)}%)
                      </span>
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

export default StudentDashboard;
