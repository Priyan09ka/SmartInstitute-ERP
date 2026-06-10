import { Link } from "react-router-dom"

function Sidebar({role}){

return(

<div className="w-60 bg-indigo-700 text-white min-h-screen p-4">

<h2 className="text-2xl font-bold mb-8">
ERP Menu
</h2>

<ul className="space-y-4">

{role === "SUPER_ADMIN" && (

<>
<li>
<Link to="/institutes" className="block hover:bg-indigo-600 p-2 rounded">
Institutes
</Link>
</li>

<li>
<Link to="/platform-users" className="block hover:bg-indigo-600 p-2 rounded">
Platform Users
</Link>
</li>
</>

)}

{role === "INSTITUTE_ADMIN" && (

<>

<li>
<Link to="/institute-admin" className="block hover:bg-indigo-600 p-2 rounded">
Dashboard
</Link>
</li>

<li>
<Link to="/students" className="block hover:bg-indigo-600 p-2 rounded">
Students
</Link>
</li>

<li>
<Link to="/teachers" className="block hover:bg-indigo-600 p-2 rounded">
Teachers
</Link>
</li>

<li>
<Link to="/subjects" className="block hover:bg-indigo-600 p-2 rounded">
Subjects
</Link>
</li>

<li>
<Link to="/attendance" className="block hover:bg-indigo-600 p-2 rounded">
Attendance
</Link>
</li>

<li>
<Link to="/quiz" className="block hover:bg-indigo-600 p-2 rounded">
Quiz
</Link>
</li>

</>

)}

</ul>

</div>

)

}

export default Sidebar