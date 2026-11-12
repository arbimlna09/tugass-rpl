<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.praktikum.rmi.model.Mahasiswa" %>
<%
    if (request.getAttribute("statusKoneksi") == null) {
        response.sendRedirect(request.getContextPath() + "/mahasiswa");
        return;
    }

    Boolean statusKoneksi = (Boolean) request.getAttribute("statusKoneksi");
    String errorMessage = (String) request.getAttribute("errorMessage");
    List<Mahasiswa> daftarMahasiswa = (List<Mahasiswa>) request.getAttribute("daftarMahasiswa");
    Integer jumlahMahasiswa = (Integer) request.getAttribute("jumlahMahasiswa");
    Mahasiswa editMahasiswa = (Mahasiswa) request.getAttribute("editMahasiswa");
    String queryNim = (String) request.getAttribute("queryNim");
    String queryNama = (String) request.getAttribute("queryNama");
    String feedbackMsg = (String) request.getAttribute("feedbackMsg");
    String feedbackType = (String) request.getAttribute("feedbackType");

    Boolean adminLogged = (Boolean) session.getAttribute("adminLogged");
    if (adminLogged == null) adminLogged = false;

    if (statusKoneksi == null) statusKoneksi = false;
    if (jumlahMahasiswa == null) jumlahMahasiswa = 0;
    if (queryNim == null) queryNim = "";
    if (queryNama == null) queryNama = "";
%>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Praktikum Java RMI - Web Monitoring Mahasiswa</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background-color: #f8fafc;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #1e293b;
        }
        .navbar-custom {
            background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
        }
        .card-custom {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -1px rgba(0, 0, 0, 0.04);
        }
        .card-header-custom {
            background-color: #ffffff;
            border-bottom: 1px solid #e2e8f0;
            font-weight: 600;
        }
        .status-badge {
            font-size: 0.88rem;
            padding: 8px 14px;
            border-radius: 50px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-weight: 600;
        }
        .status-pulse {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            display: inline-block;
        }
        .status-online {
            background-color: #dcfce7;
            color: #166534;
            border: 1px solid #86efac;
        }
        .status-online .status-pulse {
            background-color: #22c55e;
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
            animation: pulse-green 1.8s infinite;
        }
        .status-offline {
            background-color: #fee2e2;
            color: #991b1b;
            border: 1px solid #fca5a5;
        }
        .status-offline .status-pulse {
            background-color: #ef4444;
            box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
            animation: pulse-red 1.8s infinite;
        }
        @keyframes pulse-green {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(34, 197, 94, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
        }
        @keyframes pulse-red {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
        }
    </style>
</head>
<body>

    <!-- Top Navigation Bar -->
    <nav class="navbar navbar-dark navbar-custom mb-4 shadow-sm">
        <div class="container">
            <span class="navbar-brand mb-0 h1 d-flex align-items-center gap-2">
                <i class="bi bi-diagram-3-fill"></i>
                Praktikum Pemrograman Jaringan - Sesi 7 (Java RMI & Web Servlet)
            </span>
            <div class="d-flex align-items-center gap-2">
                <% if (adminLogged) { %>
                    <span class="badge bg-warning text-dark px-3 py-2 fw-semibold">
                        <i class="bi bi-shield-check"></i> Mode Admin Aktif
                    </span>
                    <form action="<%= request.getContextPath() %>/mahasiswa" method="POST" class="m-0">
                        <input type="hidden" name="action" value="logout">
                        <button type="submit" class="btn btn-sm btn-outline-light">
                            <i class="bi bi-box-arrow-right"></i> Logout
                        </button>
                    </form>
                <% } else { %>
                    <button type="button" class="btn btn-sm btn-light" data-bs-toggle="modal" data-bs-target="#loginModal">
                        <i class="bi bi-person-lock"></i> Login Admin
                    </button>
                <% } %>
            </div>
        </div>
    </nav>

    <div class="container pb-5">

        <!-- Status Server Header -->
        <div class="row mb-4 align-items-center">
            <div class="col-md-7">
                <h4 class="mb-1 fw-bold text-dark">Aplikasi Web Monitoring Data Mahasiswa (Java RMI)</h4>
                <p class="text-muted mb-0">Arsitektur 3-Tier: Browser ➔ Web Servlet (Tomcat) ➔ Java RMI Registry (Port 1099)</p>
            </div>
            <div class="col-md-5 text-md-end mt-2 mt-md-0">
                <% if (statusKoneksi) { %>
                    <div class="status-badge status-online">
                        <span class="status-pulse"></span>
                        <span>RMI Server: TERHUBUNG (Port 1099)</span>
                    </div>
                <% } else { %>
                    <div class="status-badge status-offline">
                        <span class="status-pulse"></span>
                        <span>RMI Server: KONEKSI GAGAL (Port 1099)</span>
                    </div>
                <% } %>
            </div>
        </div>

        <!-- Banner Error Jika Server Offline -->
        <% if (!statusKoneksi) { %>
            <div class="alert alert-danger card-custom p-4 mb-4 border-danger" role="alert">
                <div class="d-flex align-items-center mb-2">
                    <i class="bi bi-exclamation-triangle-fill fs-2 me-3 text-danger"></i>
                    <div>
                        <h5 class="alert-heading fw-bold mb-1">Peringatan: Gagal Menghubungi Java RMI Server!</h5>
                        <p class="mb-0"><%= errorMessage != null ? errorMessage : "RMI Server pada localhost:1099 tidak merespons." %></p>
                    </div>
                </div>
                <hr>
                <p class="mb-0 small">
                    <i class="bi bi-info-circle"></i> Pastikan RMI Server telah dijalankan dengan perintah <code>mvn exec:java</code> atau class <code>ServerApp.java</code> di terminal terpisah.
                </p>
            </div>
        <% } %>

        <!-- Alert Pesan Feedback -->
        <% if (feedbackMsg != null && !feedbackMsg.isEmpty()) { %>
            <div class="alert alert-<%= feedbackType != null ? feedbackType : "info" %> alert-dismissible fade show card-custom" role="alert">
                <i class="bi bi-info-circle-fill me-2"></i>
                <%= feedbackMsg %>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        <% } %>

        <div class="row g-4">
            <!-- Kolom Kiri: Form Input / Update & Counter -->
            <div class="col-lg-4">
                <!-- Info Statistik Jumlah Mahasiswa Otomatis (HOTS #4) -->
                <div class="card card-custom p-3 mb-4 bg-white">
                    <div class="d-flex align-items-center">
                        <div class="bg-primary bg-opacity-10 text-primary p-3 rounded-circle me-3">
                            <i class="bi bi-people-fill fs-3"></i>
                        </div>
                        <div>
                            <span class="text-muted small text-uppercase fw-bold">Total Mahasiswa Terdaftar</span>
                            <h3 class="fw-bold mb-0 text-primary"><%= jumlahMahasiswa %></h3>
                        </div>
                    </div>
                </div>

                <!-- Form Tambah atau Edit Mahasiswa (HOTS CRUD) -->
                <div class="card card-custom">
                    <div class="card-header card-header-custom py-3 d-flex justify-content-between align-items-center">
                        <span>
                            <i class="bi <%= editMahasiswa != null ? "bi-pencil-square text-warning" : "bi-person-plus-fill text-primary" %> me-2"></i>
                            <%= editMahasiswa != null ? "Edit Data Mahasiswa" : "Tambah Mahasiswa Baru" %>
                        </span>
                        <% if (editMahasiswa != null) { %>
                            <a href="<%= request.getContextPath() %>/mahasiswa" class="btn btn-sm btn-link text-decoration-none">Batal</a>
                        <% } %>
                    </div>
                    <div class="card-body">
                        <form action="<%= request.getContextPath() %>/mahasiswa" method="POST">
                            <input type="hidden" name="action" value="<%= editMahasiswa != null ? "update" : "tambah" %>">
                            <div class="mb-3">
                                <label for="nim" class="form-label fw-semibold">NIM (Nomor Induk Mahasiswa)</label>
                                <input type="text" class="form-control" id="nim" name="nim"
                                       value="<%= editMahasiswa != null ? editMahasiswa.getNim() : "" %>"
                                       placeholder="Contoh: 220005" required
                                       <%= editMahasiswa != null ? "readonly" : "" %>
                                       <%= !statusKoneksi ? "disabled" : "" %>>
                                <% if (editMahasiswa != null) { %>
                                    <small class="text-muted">NIM bersifat permanen (Primary Key).</small>
                                <% } %>
                            </div>
                            <div class="mb-3">
                                <label for="nama" class="form-label fw-semibold">Nama Lengkap</label>
                                <input type="text" class="form-control" id="nama" name="nama"
                                       value="<%= editMahasiswa != null ? editMahasiswa.getNama() : "" %>"
                                       placeholder="Contoh: Muhammad Rizki" required
                                       <%= !statusKoneksi ? "disabled" : "" %>>
                            </div>
                            <div class="mb-3">
                                <label for="jurusan" class="form-label fw-semibold">Program Studi / Jurusan</label>
                                <select class="form-select" id="jurusan" name="jurusan" required <%= !statusKoneksi ? "disabled" : "" %>>
                                    <option value="" disabled <%= editMahasiswa == null ? "selected" : "" %>>Pilih Jurusan...</option>
                                    <option value="Teknik Informatika" <%= (editMahasiswa != null && "Teknik Informatika".equals(editMahasiswa.getJurusan())) ? "selected" : "" %>>Teknik Informatika</option>
                                    <option value="Sistem Informasi" <%= (editMahasiswa != null && "Sistem Informasi".equals(editMahasiswa.getJurusan())) ? "selected" : "" %>>Sistem Informasi</option>
                                    <option value="Teknik Komputer" <%= (editMahasiswa != null && "Teknik Komputer".equals(editMahasiswa.getJurusan())) ? "selected" : "" %>>Teknik Komputer</option>
                                    <option value="Rekayasa Perangkat Lunak" <%= (editMahasiswa != null && "Rekayasa Perangkat Lunak".equals(editMahasiswa.getJurusan())) ? "selected" : "" %>>Rekayasa Perangkat Lunak</option>
                                    <option value="Teknologi Informasi" <%= (editMahasiswa != null && "Teknologi Informasi".equals(editMahasiswa.getJurusan())) ? "selected" : "" %>>Teknologi Informasi</option>
                                </select>
                            </div>
                            <button type="submit" class="btn <%= editMahasiswa != null ? "btn-warning" : "btn-primary" %> w-100 py-2 fw-semibold" <%= !statusKoneksi ? "disabled" : "" %>>
                                <i class="bi <%= editMahasiswa != null ? "bi-check2-circle" : "bi-save2" %> me-1"></i>
                                <%= editMahasiswa != null ? "Perbarui di RMI Server" : "Simpan ke Server RMI" %>
                            </button>
                        </form>
                    </div>
                </div>
            </div>

            <!-- Kolom Kanan: Pencarian NIM/Nama & Tabel Data Mahasiswa -->
            <div class="col-lg-8">
                <!-- Card Pencarian (NIM & Nama HOTS) -->
                <div class="card card-custom mb-4">
                    <div class="card-header card-header-custom py-3">
                        <i class="bi bi-search text-primary me-2"></i> Filter & Pencarian Data Mahasiswa
                    </div>
                    <div class="card-body">
                        <form action="<%= request.getContextPath() %>/mahasiswa" method="GET" class="row g-2">
                            <div class="col-md-5">
                                <div class="input-group">
                                    <span class="input-group-text"><i class="bi bi-card-heading"></i></span>
                                    <input type="text" name="nim" class="form-control" placeholder="Cari berdasarkan NIM..." value="<%= queryNim %>" <%= !statusKoneksi ? "disabled" : "" %>>
                                </div>
                            </div>
                            <div class="col-md-5">
                                <div class="input-group">
                                    <span class="input-group-text"><i class="bi bi-person"></i></span>
                                    <input type="text" name="nama" class="form-control" placeholder="Cari berdasarkan Nama (HOTS)..." value="<%= queryNama %>" <%= !statusKoneksi ? "disabled" : "" %>>
                                </div>
                            </div>
                            <div class="col-md-2 d-grid">
                                <button type="submit" class="btn btn-outline-primary" <%= !statusKoneksi ? "disabled" : "" %>>
                                    <i class="bi bi-search"></i> Cari
                                </button>
                            </div>
                        </form>
                        <% if (!queryNim.isEmpty() || !queryNama.isEmpty()) { %>
                            <div class="mt-2 d-flex align-items-center justify-content-between">
                                <span class="badge bg-secondary">
                                    Filter aktif: <%= !queryNim.isEmpty() ? "NIM '" + queryNim + "'" : "" %> <%= !queryNama.isEmpty() ? "Nama '" + queryNama + "'" : "" %>
                                </span>
                                <a href="<%= request.getContextPath() %>/mahasiswa" class="btn btn-sm btn-link text-decoration-none p-0">
                                    <i class="bi bi-arrow-counterclockwise"></i> Reset / Tampilkan Semua
                                </a>
                            </div>
                        <% } %>
                    </div>
                </div>

                <!-- Tabel Data Mahasiswa -->
                <div class="card card-custom">
                    <div class="card-header card-header-custom py-3 d-flex justify-content-between align-items-center">
                        <span><i class="bi bi-table text-primary me-2"></i> Daftar Mahasiswa (Data dari Java RMI)</span>
                        <a href="<%= request.getContextPath() %>/mahasiswa" class="btn btn-sm btn-outline-secondary" title="Muat Ulang Data">
                            <i class="bi bi-arrow-clockwise"></i> Refresh
                        </a>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th style="width: 50px;" class="text-center">No</th>
                                        <th style="width: 130px;">NIM</th>
                                        <th>Nama Lengkap</th>
                                        <th>Jurusan</th>
                                        <th style="width: 140px;" class="text-center">Aksi (CRUD)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (daftarMahasiswa != null && !daftarMahasiswa.isEmpty()) { %>
                                        <% int no = 1; for (Mahasiswa mhs : daftarMahasiswa) { %>
                                            <tr>
                                                <td class="text-center fw-semibold text-muted"><%= no++ %></td>
                                                <td><span class="badge bg-light text-dark border font-monospace px-2 py-1"><%= mhs.getNim() %></span></td>
                                                <td class="fw-semibold text-dark"><%= mhs.getNama() %></td>
                                                <td><span class="badge bg-primary bg-opacity-10 text-primary px-3 py-2 rounded-pill"><%= mhs.getJurusan() %></span></td>
                                                <td class="text-center">
                                                    <!-- Tombol Edit (HOTS Update) -->
                                                    <a href="<%= request.getContextPath() %>/mahasiswa?editNim=<%= mhs.getNim() %>" class="btn btn-sm btn-outline-warning me-1" title="Edit Data">
                                                        <i class="bi bi-pencil"></i>
                                                    </a>
                                                    <!-- Tombol Hapus (Delete) -->
                                                    <form action="<%= request.getContextPath() %>/mahasiswa" method="POST" onsubmit="return confirm('Apakah Anda yakin ingin menghapus mahasiswa <%= mhs.getNama() %> (NIM: <%= mhs.getNim() %>)?');" style="display:inline;">
                                                        <input type="hidden" name="action" value="hapus">
                                                        <input type="hidden" name="nim" value="<%= mhs.getNim() %>">
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" title="Hapus Data">
                                                            <i class="bi bi-trash3-fill"></i>
                                                        </button>
                                                    </form>
                                                </td>
                                            </tr>
                                        <% } %>
                                    <% } else { %>
                                        <tr>
                                            <td colspan="5" class="text-center py-5 text-muted">
                                                <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                                                <%= statusKoneksi ? "Tidak ada data mahasiswa yang cocok." : "Tidak dapat menampilkan data karena Java RMI Server offline." %>
                                            </td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

            </div>
        </div>

        <!-- Footer -->
        <footer class="mt-5 text-center text-muted small">
            <hr class="mb-3">
            <p class="mb-0">Praktikum Pemrograman Jaringan Sesi 7 &copy; 2026 - Arsitektur RMI Multi-Tier</p>
        </footer>
    </div>

    <!-- Modal Login Administrator (HOTS #1) -->
    <div class="modal fade" id="loginModal" tabindex="-1" aria-labelledby="loginModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content card-custom">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold" id="loginModalLabel"><i class="bi bi-shield-lock text-primary me-2"></i>Login Administrator</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="<%= request.getContextPath() %>/mahasiswa" method="POST">
                    <input type="hidden" name="action" value="login">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="username" class="form-label fw-semibold">Username</label>
                            <input type="text" class="form-control" id="username" name="username" placeholder="admin" required>
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label fw-semibold">Password</label>
                            <input type="password" class="form-control" id="password" name="password" placeholder="admin123" required>
                            <small class="text-muted">Kredensial default: <code>admin</code> / <code>admin123</code></small>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Tutup</button>
                        <button type="submit" class="btn btn-primary">Login</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Bootstrap 5 Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
