package com.praktikum.rmi.web;

import com.praktikum.rmi.model.Mahasiswa;
import com.praktikum.rmi.service.MahasiswaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Standalone Web Server Runner (Port 8080)
 * Memungkinkan aplikasi web RMI dijalankan langsung dari terminal tanpa harus menginstal Apache Tomcat manual.
 * Menggunakan built-in Java HTTP Server dan menyajikan UI HTML/Bootstrap 5 yang memanggil layanan Java RMI.
 */
public class WebServerApp {
    public static final int HTTP_PORT = 8080;
    public static final String RMI_HOST = "localhost";
    public static final int RMI_PORT = 1099;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);

        server.createContext("/", exchange -> {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                exchange.getResponseHeaders().set("Location", "/mahasiswa");
                exchange.sendResponseHeaders(302, -1);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
            exchange.close();
        });

        server.createContext("/mahasiswa", new MahasiswaWebHandler());
        server.setExecutor(null); // default executor
        server.start();

        System.out.println("==========================================================");
        System.out.println("🌐 [WEB SERVER JAVA] Berjalan di port " + HTTP_PORT);
        System.out.println("🔗 Buka di Browser : http://localhost:" + HTTP_PORT + "/mahasiswa");
        System.out.println("🎯 Terhubung ke RMI Server : rmi://" + RMI_HOST + ":" + RMI_PORT + "/MahasiswaService");
        System.out.println("==========================================================");
        System.out.println("Silakan buka browser Anda di http://localhost:" + HTTP_PORT + "/mahasiswa");
    }

    static class MahasiswaWebHandler implements HttpHandler {
        private MahasiswaService getService() {
            try {
                Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
                return (MahasiswaService) registry.lookup("MahasiswaService");
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
            } else {
                handleGet(exchange);
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            String body = baos.toString(StandardCharsets.UTF_8.name());
            Map<String, String> params = parseFormData(body);

            String action = params.get("action");
            MahasiswaService service = getService();

            String msg = "";
            String msgType = "info";

            if (service == null) {
                msg = "Gagal menghubungi RMI Server!";
                msgType = "danger";
            } else {
                try {
                    if ("tambah".equalsIgnoreCase(action)) {
                        String nim = params.get("nim");
                        String nama = params.get("nama");
                        String jurusan = params.get("jurusan");
                        if (nim != null && nama != null && jurusan != null && !nim.trim().isEmpty()) {
                            boolean ok = service.tambahMahasiswa(new Mahasiswa(nim.trim(), nama.trim(), jurusan.trim()));
                            if (ok) {
                                msg = "Mahasiswa NIM " + nim + " berhasil ditambahkan ke RMI Server!";
                                msgType = "success";
                            } else {
                                msg = "Gagal! NIM " + nim + " sudah terdaftar di server.";
                                msgType = "danger";
                            }
                        }
                    } else if ("update".equalsIgnoreCase(action)) {
                        String nim = params.get("nim");
                        String nama = params.get("nama");
                        String jurusan = params.get("jurusan");
                        boolean ok = service.updateMahasiswa(new Mahasiswa(nim.trim(), nama.trim(), jurusan.trim()));
                        if (ok) {
                            msg = "Data Mahasiswa NIM " + nim + " berhasil diperbarui!";
                            msgType = "success";
                        } else {
                            msg = "Gagal memperbarui data!";
                            msgType = "danger";
                        }
                    } else if ("hapus".equalsIgnoreCase(action)) {
                        String nim = params.get("nim");
                        boolean ok = service.hapusMahasiswa(nim.trim());
                        if (ok) {
                            msg = "Mahasiswa NIM " + nim + " berhasil dihapus dari RMI Server!";
                            msgType = "success";
                        } else {
                            msg = "Gagal menghapus mahasiswa!";
                            msgType = "danger";
                        }
                    }
                } catch (Exception e) {
                    msg = "Error RMI: " + e.getMessage();
                    msgType = "danger";
                }
            }

            exchange.getResponseHeaders().set("Location", "/mahasiswa?msg=" + urlEncode(msg) + "&msgType=" + urlEncode(msgType));
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQueryParams(query);

            String nim = queryParams.get("nim");
            String nama = queryParams.get("nama");
            String editNim = queryParams.get("editNim");
            String msg = queryParams.get("msg");
            String msgType = queryParams.get("msgType");

            MahasiswaService service = getService();
            boolean statusKoneksi = (service != null);
            List<Mahasiswa> list = new ArrayList<>();
            int total = 0;
            Mahasiswa editMhs = null;

            if (statusKoneksi) {
                try {
                    total = service.getJumlahMahasiswa();

                    if (editNim != null && !editNim.trim().isEmpty()) {
                        editMhs = service.getMahasiswaByNim(editNim.trim());
                    }

                    if (nim != null && !nim.trim().isEmpty()) {
                        Mahasiswa m = service.getMahasiswaByNim(nim.trim());
                        if (m != null) list.add(m);
                    } else if (nama != null && !nama.trim().isEmpty()) {
                        list = service.cariMahasiswaByNama(nama.trim());
                    } else {
                        list = service.getAllMahasiswa();
                    }
                } catch (Exception e) {
                    statusKoneksi = false;
                }
            }

            String html = renderHtml(statusKoneksi, list, total, editMhs, nim, nama, msg, msgType);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private String renderHtml(boolean statusKoneksi, List<Mahasiswa> list, int total, Mahasiswa editMhs,
                                  String qNim, String qNama, String msg, String msgType) {
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html lang='id'><head><meta charset='UTF-8'>");
            sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            sb.append("<title>Praktikum Java RMI - Web Monitoring Mahasiswa</title>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            sb.append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css'>");
            sb.append("<style>");
            sb.append("body { background-color: #f8fafc; font-family: -apple-system, sans-serif; color: #1e293b; }");
            sb.append(".navbar-custom { background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); }");
            sb.append(".card-custom { border: none; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07); }");
            sb.append(".card-header-custom { background: #fff; border-bottom: 1px solid #e2e8f0; font-weight: 600; }");
            sb.append(".status-badge { font-size: 0.9rem; padding: 8px 14px; border-radius: 50px; display: inline-flex; align-items: center; gap: 8px; font-weight: 600; }");
            sb.append(".status-pulse { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }");
            sb.append(".status-online { background-color: #dcfce7; color: #166534; border: 1px solid #86efac; }");
            sb.append(".status-online .status-pulse { background-color: #22c55e; }");
            sb.append(".status-offline { background-color: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; }");
            sb.append(".status-offline .status-pulse { background-color: #ef4444; }");
            sb.append("</style></head><body>");

            // Navbar
            sb.append("<nav class='navbar navbar-dark navbar-custom mb-4 shadow-sm'>");
            sb.append("<div class='container'>");
            sb.append("<span class='navbar-brand mb-0 h1 d-flex align-items-center gap-2'>");
            sb.append("<i class='bi bi-diagram-3-fill'></i> Praktikum Pemrograman Jaringan - Sesi 7 (Java RMI & Web)");
            sb.append("</span>");
            sb.append("<span class='badge bg-light text-dark px-3 py-2'><i class='bi bi-hdd-network'></i> Web: Port 8080 | Java RMI: Port 1099</span>");
            sb.append("</div></nav>");

            sb.append("<div class='container pb-5'>");

            // Header & Status
            sb.append("<div class='row mb-4 align-items-center'>");
            sb.append("<div class='col-md-7'>");
            sb.append("<h4 class='mb-1 fw-bold text-dark'>Aplikasi Web Monitoring Data Mahasiswa (Java RMI)</h4>");
            sb.append("<p class='text-muted mb-0'>Arsitektur 3-Tier: Browser ➔ Web Controller ➔ Java RMI Registry (Port 1099)</p>");
            sb.append("</div>");
            sb.append("<div class='col-md-5 text-md-end mt-2 mt-md-0'>");
            if (statusKoneksi) {
                sb.append("<div class='status-badge status-online'><span class='status-pulse'></span><span>RMI Server: TERHUBUNG (Port 1099)</span></div>");
            } else {
                sb.append("<div class='status-badge status-offline'><span class='status-pulse'></span><span>RMI Server: KONEKSI GAGAL (Port 1099)</span></div>");
            }
            sb.append("</div></div>");

            // Alert Offline
            if (!statusKoneksi) {
                sb.append("<div class='alert alert-danger card-custom p-4 mb-4 border-danger' role='alert'>");
                sb.append("<h5 class='alert-heading fw-bold mb-1'><i class='bi bi-exclamation-triangle-fill me-2'></i>Peringatan: Gagal Menghubungi Java RMI Server!</h5>");
                sb.append("<p class='mb-0'>RMI Server pada localhost:1099 tidak merespons atau sedang offline.</p>");
                sb.append("<hr><p class='mb-0 small'><i class='bi bi-info-circle'></i> Pastikan RMI Server telah dijalankan dengan perintah <code>java -cp target/classes com.praktikum.rmi.server.ServerApp</code> di terminal terpisah.</p>");
                sb.append("</div>");
            }

            // Flash Feedback
            if (msg != null && !msg.isEmpty()) {
                String type = (msgType != null && !msgType.isEmpty()) ? msgType : "info";
                sb.append("<div class='alert alert-").append(escape(type)).append(" alert-dismissible fade show card-custom' role='alert'>");
                sb.append("<i class='bi bi-info-circle-fill me-2'></i>").append(escape(msg));
                sb.append("<button type='button' class='btn-close' data-bs-dismiss='alert'></button></div>");
            }

            sb.append("<div class='row g-4'>");

            // Kolom Kiri
            sb.append("<div class='col-lg-4'>");
            // Card Counter
            sb.append("<div class='card card-custom p-3 mb-4 bg-white'><div class='d-flex align-items-center'>");
            sb.append("<div class='bg-primary bg-opacity-10 text-primary p-3 rounded-circle me-3'><i class='bi bi-people-fill fs-3'></i></div>");
            sb.append("<div><span class='text-muted small text-uppercase fw-bold'>Total Mahasiswa Terdaftar</span>");
            sb.append("<h3 class='fw-bold mb-0 text-primary'>").append(total).append("</h3></div></div></div>");

            // Form Tambah/Edit
            boolean isEdit = (editMhs != null);
            sb.append("<div class='card card-custom'>");
            sb.append("<div class='card-header card-header-custom py-3 d-flex justify-content-between align-items-center'>");
            sb.append("<span><i class='bi ").append(isEdit ? "bi-pencil-square text-warning" : "bi-person-plus-fill text-primary").append(" me-2'></i>");
            sb.append(isEdit ? "Edit Data Mahasiswa" : "Tambah Mahasiswa Baru").append("</span>");
            if (isEdit) sb.append("<a href='/mahasiswa' class='btn btn-sm btn-link text-decoration-none'>Batal</a>");
            sb.append("</div><div class='card-body'>");
            sb.append("<form action='/mahasiswa' method='POST'>");
            sb.append("<input type='hidden' name='action' value='").append(isEdit ? "update" : "tambah").append("'>");
            sb.append("<div class='mb-3'><label class='form-label fw-semibold'>NIM</label>");
            sb.append("<input type='text' class='form-control' name='nim' placeholder='Contoh: 220005' value='")
              .append(isEdit ? escape(editMhs.getNim()) : "").append("' required ").append(isEdit ? "readonly " : "")
              .append(!statusKoneksi ? "disabled" : "").append("></div>");

            sb.append("<div class='mb-3'><label class='form-label fw-semibold'>Nama Lengkap</label>");
            sb.append("<input type='text' class='form-control' name='nama' placeholder='Contoh: Muhammad Rizki' value='")
              .append(isEdit ? escape(editMhs.getNama()) : "").append("' required ").append(!statusKoneksi ? "disabled" : "").append("></div>");

            sb.append("<div class='mb-3'><label class='form-label fw-semibold'>Jurusan</label>");
            sb.append("<select class='form-select' name='jurusan' required ").append(!statusKoneksi ? "disabled" : "").append(">");
            String[] jurusans = {"Teknik Informatika", "Sistem Informasi", "Teknik Komputer", "Rekayasa Perangkat Lunak", "Teknologi Informasi"};
            for (String j : jurusans) {
                boolean sel = isEdit && j.equals(editMhs.getJurusan());
                sb.append("<option value='").append(j).append("' ").append(sel ? "selected" : "").append(">").append(j).append("</option>");
            }
            sb.append("</select></div>");

            sb.append("<button type='submit' class='btn ").append(isEdit ? "btn-warning" : "btn-primary")
              .append(" w-100 py-2 fw-semibold' ").append(!statusKoneksi ? "disabled" : "").append(">");
            sb.append("<i class='bi ").append(isEdit ? "bi-check2-circle" : "bi-save2").append(" me-1'></i>");
            sb.append(isEdit ? "Perbarui di RMI Server" : "Simpan ke Server RMI").append("</button>");
            sb.append("</form></div></div></div>");

            // Kolom Kanan
            sb.append("<div class='col-lg-8'>");
            // Card Search
            sb.append("<div class='card card-custom mb-4'>");
            sb.append("<div class='card-header card-header-custom py-3'><i class='bi bi-search text-primary me-2'></i> Filter & Pencarian Data</div>");
            sb.append("<div class='card-body'><form action='/mahasiswa' method='GET' class='row g-2'>");
            sb.append("<div class='col-md-5'><input type='text' name='nim' class='form-control' placeholder='Cari berdasarkan NIM...' value='").append(qNim != null ? escape(qNim) : "").append("' ").append(!statusKoneksi ? "disabled" : "").append("></div>");
            sb.append("<div class='col-md-5'><input type='text' name='nama' class='form-control' placeholder='Cari berdasarkan Nama (HOTS)...' value='").append(qNama != null ? escape(qNama) : "").append("' ").append(!statusKoneksi ? "disabled" : "").append("></div>");
            sb.append("<div class='col-md-2 d-grid'><button type='submit' class='btn btn-outline-primary' ").append(!statusKoneksi ? "disabled" : "").append(">Cari</button></div>");
            sb.append("</form>");
            if ((qNim != null && !qNim.isEmpty()) || (qNama != null && !qNama.isEmpty())) {
                sb.append("<div class='mt-2'><a href='/mahasiswa' class='btn btn-sm btn-link text-decoration-none p-0'><i class='bi bi-arrow-counterclockwise'></i> Reset / Tampilkan Semua</a></div>");
            }
            sb.append("</div></div>");

            // Tabel
            sb.append("<div class='card card-custom'>");
            sb.append("<div class='card-header card-header-custom py-3 d-flex justify-content-between align-items-center'>");
            sb.append("<span><i class='bi bi-table text-primary me-2'></i> Daftar Data Mahasiswa (Data dari Java RMI Server)</span>");
            sb.append("<a href='/mahasiswa' class='btn btn-sm btn-outline-secondary'><i class='bi bi-arrow-clockwise'></i> Refresh</a>");
            sb.append("</div><div class='card-body p-0'><div class='table-responsive'>");
            sb.append("<table class='table table-hover align-middle mb-0'><thead class='table-light'><tr>");
            sb.append("<th class='text-center' style='width:50px;'>No</th><th style='width:130px;'>NIM</th><th>Nama Lengkap</th><th>Jurusan</th><th class='text-center' style='width:130px;'>Aksi</th>");
            sb.append("</tr></thead><tbody>");

            if (list != null && !list.isEmpty()) {
                int no = 1;
                for (Mahasiswa m : list) {
                    sb.append("<tr>");
                    sb.append("<td class='text-center fw-semibold text-muted'>").append(no++).append("</td>");
                    sb.append("<td><span class='badge bg-light text-dark border font-monospace'>").append(escape(m.getNim())).append("</span></td>");
                    sb.append("<td class='fw-semibold'>").append(escape(m.getNama())).append("</td>");
                    sb.append("<td><span class='badge bg-primary bg-opacity-10 text-primary rounded-pill px-3 py-2'>").append(escape(m.getJurusan())).append("</span></td>");
                    sb.append("<td class='text-center'>");
                    sb.append("<a href='/mahasiswa?editNim=").append(escape(m.getNim())).append("' class='btn btn-sm btn-outline-warning me-1' title='Edit'><i class='bi bi-pencil'></i></a>");
                    sb.append("<form action='/mahasiswa' method='POST' style='display:inline;' onsubmit=\"return confirm('Hapus mahasiswa ").append(escape(m.getNama())).append("?');\">");
                    sb.append("<input type='hidden' name='action' value='hapus'>");
                    sb.append("<input type='hidden' name='nim' value='").append(escape(m.getNim())).append("'>");
                    sb.append("<button type='submit' class='btn btn-sm btn-outline-danger' title='Hapus'><i class='bi bi-trash3-fill'></i></button>");
                    sb.append("</form></td></tr>");
                }
            } else {
                sb.append("<tr><td colspan='5' class='text-center py-5 text-muted'>");
                sb.append("<i class='bi bi-inbox fs-1 d-block mb-2'></i>");
                sb.append(statusKoneksi ? "Tidak ada data mahasiswa ditemukan." : "RMI Server sedang offline.");
                sb.append("</td></tr>");
            }

            sb.append("</tbody></table></div></div></div></div></div>");
            sb.append("<footer class='mt-5 text-center text-muted small'><hr><p>Praktikum Pemrograman Jaringan Sesi 7 &copy; 2026 - Java Remote Method Invocation & Web</p></footer>");
            sb.append("</div><script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js'></script></body></html>");
            return sb.toString();
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }

        private static String urlEncode(String s) {
            try { return java.net.URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8.name()); }
            catch (Exception e) { return ""; }
        }

        private static Map<String, String> parseQueryParams(String query) {
            Map<String, String> map = new HashMap<>();
            if (query == null || query.isEmpty()) return map;
            for (String pair : query.split("&")) {
                int idx = pair.indexOf("=");
                try {
                    String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()) : pair;
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : "";
                    map.put(key, value);
                } catch (Exception ignored) {}
            }
            return map;
        }

        private static Map<String, String> parseFormData(String body) {
            return parseQueryParams(body);
        }
    }
}
