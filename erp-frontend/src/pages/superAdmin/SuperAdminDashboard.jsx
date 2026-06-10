import { useEffect, useState } from "react";
import { apiRequest } from "../../services/Api";

function SuperAdminDashboard() {
  const [institutes, setInstitutes] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchInstitutes = async () => {
    try {
      setLoading(true);
      const data = await apiRequest("/platform/institutes");
      setInstitutes(data);
    } catch (err) {
      alert("Failed to load institutes");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInstitutes();
  }, []);

  const toggleInstitute = async (id) => {
    if (!window.confirm("Change status?")) return;

    try {
      await apiRequest(`/platform/institutes/${id}/toggle`, "PUT");
      fetchInstitutes();
    } catch {
      alert("Failed");
    }
  };

  return (
    <div className="p-6">
        <h1 className="text-3xl font-bold mb-6">Dashboard</h1>

        {/* ===== INSTITUTES ===== */}
        <div className="bg-white shadow rounded-xl">
          <h2 className="text-xl font-semibold p-4">Institutes</h2>

          {loading ? (
            <p className="p-4">Loading...</p>
          ) : (
            <table className="w-full text-left">
              <thead className="bg-indigo-600 text-white">
                <tr>
                  <th className="p-3">Name</th>
                  <th>Email</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {institutes.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="p-4 text-center">
                      No data
                    </td>
                  </tr>
                ) : (
                  institutes.map((inst) => (
                    <tr key={inst.id} className="border-b">
                      <td className="p-3">{inst.name}</td>
                      <td>{inst.email || "-"}</td>

                      <td>
                        <span
                          className={`px-2 py-1 rounded text-sm ${
                            inst.active
                              ? "bg-green-100 text-green-700"
                              : "bg-red-100 text-red-700"
                          }`}
                        >
                          {inst.active ? "Active" : "Inactive"}
                        </span>
                      </td>

                      <td>
                        <button
                          onClick={() => toggleInstitute(inst.id)}
                          className="bg-indigo-600 text-white px-3 py-1 rounded"
                        >
                          Toggle
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
    </div>
  );
}

export default SuperAdminDashboard;