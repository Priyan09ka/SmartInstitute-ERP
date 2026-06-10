import { useEffect, useRef, useState } from "react";
import { apiRequest } from "../services/Api";

function Institutes() {
  const [institutes, setInstitutes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [users, setUsers] = useState(null);
  const [loading, setLoading] = useState(false);
  const [usersError, setUsersError] = useState(null);
  const usersSectionRef = useRef(null);

  const loadInstitutes = async () => {
    try {
      const data = await apiRequest("/platform/institutes");
      setInstitutes(data);
    } catch (err) {
      alert("Failed to load institutes");
    }
  };

  useEffect(() => {
    loadInstitutes();
  }, []);

  const loadInstituteUsers = async (instituteId) => {
    if (selectedId === instituteId) {
      setSelectedId(null);
      setUsers(null);
      setUsersError(null);
      return;
    }
    setLoading(true);
    setUsersError(null);
    setUsers(null);
    setSelectedId(instituteId);
    try {
      const data = await apiRequest(`/platform/institutes/${instituteId}/users`);
      setUsers(data || { instituteId: instituteId, instituteName: "", students: [], teachers: [], principals: [] });
      setUsersError(null);
      setTimeout(() => usersSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }), 100);
    } catch (err) {
      setUsersError(err.message || "Failed to load users.");
      setUsers(null);
    } finally {
      setLoading(false);
    }
  };

  const allUsers = users
    ? [
        ...(Array.isArray(users.students) ? users.students : []).map((u) => ({ ...u, role: "Student" })),
        ...(Array.isArray(users.teachers) ? users.teachers : []).map((u) => ({ ...u, role: "Teacher" })),
        ...(Array.isArray(users.principals) ? users.principals : []).map((u) => ({ ...u, role: "Principal" })),
      ]
    : [];
  const selectedInstitute = institutes.find((i) => i.id === selectedId);
  const showUsersSection = selectedId != null && !loading;

  return (
    <div className="p-6 bg-gray-100 min-h-full">
          <h1 className="text-2xl font-bold mb-6">Institutes & Users</h1>

          <div className="bg-white shadow rounded mb-6 overflow-hidden">
            <h2 className="text-lg font-semibold p-4 bg-indigo-600 text-white">
              Institutes
            </h2>
            <table className="w-full">
              <thead className="bg-gray-200">
                <tr>
                  <th className="p-3 text-left">Name</th>
                  <th className="p-3 text-left">Status</th>
                  <th className="p-3 text-left">Action</th>
                </tr>
              </thead>
              <tbody>
                {institutes.length === 0 ? (
                  <tr>
                    <td colSpan="3" className="p-4 text-center">
                      No institutes
                    </td>
                  </tr>
                ) : (
                  institutes.map((inst) => (
                    <tr key={inst.id} className="border-t hover:bg-gray-50">
                      <td className="p-3">{inst.name}</td>
                      <td className="p-3">
                        <span
                          className={`px-2 py-0.5 rounded text-sm ${
                            inst.active
                              ? "bg-green-100 text-green-700"
                              : "bg-red-100 text-red-700"
                          }`}
                        >
                          {inst.active ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td className="p-3">
                        <button
                          onClick={() => loadInstituteUsers(inst.id)}
                          className={`px-3 py-1 rounded text-sm ${
                            selectedId === inst.id
                              ? "bg-indigo-800 text-white"
                              : "bg-indigo-600 text-white hover:bg-indigo-700"
                          }`}
                        >
                          {selectedId === inst.id
                            ? "Hide users"
                            : "View users"}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {loading && selectedId && (
            <p className="p-4 text-gray-600">Loading users...</p>
          )}

          {showUsersSection && (
            <div ref={usersSectionRef} className="bg-white shadow rounded overflow-hidden">
              <h2 className="text-lg font-semibold p-4 bg-gray-700 text-white">
                {selectedInstitute?.name || "Institute"} – Students, Teachers, Principals
              </h2>
              {usersError ? (
                <div className="p-4 text-red-600">
                  {usersError}
                  <button
                    type="button"
                    onClick={() => loadInstituteUsers(selectedId)}
                    className="ml-3 text-indigo-600 hover:underline"
                  >
                    Retry
                  </button>
                </div>
              ) : (
                <table className="w-full">
                  <thead className="bg-gray-200">
                    <tr>
                      <th className="p-3 text-left">Name</th>
                      <th className="p-3 text-left">Email</th>
                      <th className="p-3 text-left">Role</th>
                    </tr>
                  </thead>
                  <tbody>
                    {allUsers.length === 0 ? (
                      <tr>
                        <td colSpan="3" className="p-4 text-center text-gray-500">
                          No users in this institute
                        </td>
                      </tr>
                    ) : (
                      allUsers.map((u) => (
                        <tr key={`${u.role}-${u.id}`} className="border-t">
                          <td className="p-3">{u.name ?? "-"}</td>
                          <td className="p-3">{u.email ?? "-"}</td>
                          <td className="p-3">
                            <span
                              className={`px-2 py-0.5 rounded text-sm ${
                                u.role === "Student"
                                  ? "bg-blue-100 text-blue-800"
                                  : u.role === "Teacher"
                                  ? "bg-amber-100 text-amber-800"
                                  : "bg-purple-100 text-purple-800"
                              }`}
                            >
                              {u.role}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              )}
            </div>
          )}
    </div>
  );
}

export default Institutes;