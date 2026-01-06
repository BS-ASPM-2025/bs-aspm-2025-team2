const fs = require("fs");
fs.mkdirSync("build", { recursive: true });
fs.writeFileSync("build/index.html", `<!doctype html>
<html><head><meta charset="utf-8"><title>SEPM</title></head>
<body>
<h1>SEPM Frontend placeholder</h1>
<p>Pipeline OK. Frontend will be implemented later.</p>
</body></html>`);
console.log("OK: build/ created");
