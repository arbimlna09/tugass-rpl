package com.praktikum.rmi.client;

import com.praktikum.rmi.model.Mahasiswa;
import com.praktikum.rmi.service.MahasiswaService;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

/**
 * RMI Client Application (Console-based Client)
 * Memenuhi spesifikasi:
 * - Soal 2: Membuat RMI Client (Koneksi, panggil remote method, tampilkan data, pesan error jika offline).
 * - F.2: Source code RMI Client.
 * - HOTS: Pengujian client-server melalui terminal maupun jaringan LAN.
 */
public class ClientApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = "localhost";
        int port = 1099;

        System.out.println("==========================================================");
        System.out.println("💻 [RMI CLIENT] Program Client Java RMI");
        System.out.println("==========================================================");
        System.out.print("Masukkan Host RMI Server (tekan Enter untuk 'localhost'): ");
        String inputHost = scanner.nextLine().trim();
        if (!inputHost.isEmpty()) {
            host = inputHost;
        }

        MahasiswaService service = null;

        try {
            System.out.println("Menghubungi RMI Registry di " + host + ":" + port + "...");
            Registry registry = LocateRegistry.getRegistry(host, port);
            service = (MahasiswaService) registry.lookup("MahasiswaService");
            System.out.println("✔ KONEKSI BERHASIL! Terhubung ke MahasiswaService pada " + host + ":" + port);
        } catch (ConnectException e) {
            System.err.println("❌ KONEKSI GAGAL: RMI Server pada " + host + ":" + port + " sedang OFFLINE atau tidak dapat dijangkau.");
            System.err.println("Detail Error: " + e.getMessage());
            return;
        } catch (NotBoundException e) {
            System.err.println("❌ ERROR: Service 'MahasiswaService' belum terdaftar di RMI Registry.");
            return;
        } catch (Exception e) {
            System.err.println("❌ Terjadi kesalahan saat inisialisasi client: " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            System.out.println("\n----------------------------------------------------------");
            System.out.println("PILIHAN MENU RMI CLIENT:");
            System.out.println("1. Tampilkan Semua Mahasiswa (getAllMahasiswa)");
            System.out.println("2. Cari Mahasiswa Berdasarkan NIM (getMahasiswaByNim)");
            System.out.println("3. Cari Mahasiswa Berdasarkan Nama [HOTS] (cariMahasiswaByNama)");
            System.out.println("4. Tambah Mahasiswa Baru (tambahMahasiswa)");
            System.out.println("5. Perbarui Data Mahasiswa [HOTS] (updateMahasiswa)");
            System.out.println("6. Hapus Mahasiswa (hapusMahasiswa)");
            System.out.println("7. Tampilkan Total Jumlah Mahasiswa (getJumlahMahasiswa)");
            System.out.println("0. Keluar");
            System.out.print("Pilih menu (0-7): ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        List<Mahasiswa> list = service.getAllMahasiswa();
                        System.out.println("\n--- DAFTAR SELURUH MAHASISWA (Total: " + list.size() + ") ---");
                        if (list.isEmpty()) {
                            System.out.println("(Belum ada data mahasiswa)");
                        } else {
                            for (int i = 0; i < list.size(); i++) {
                                Mahasiswa m = list.get(i);
                                System.out.printf("%d. NIM: %-10s | Nama: %-25s | Jurusan: %s\n",
                                        (i + 1), m.getNim(), m.getNama(), m.getJurusan());
                            }
                        }
                        break;

                    case "2":
                        System.out.print("Masukkan NIM yang dicari: ");
                        String searchNim = scanner.nextLine().trim();
                        Mahasiswa mhsNim = service.getMahasiswaByNim(searchNim);
                        if (mhsNim != null) {
                            System.out.println("\n✔ Data Ditemukan:");
                            System.out.println("   NIM     : " + mhsNim.getNim());
                            System.out.println("   Nama    : " + mhsNim.getNama());
                            System.out.println("   Jurusan : " + mhsNim.getJurusan());
                        } else {
                            System.out.println("❌ Mahasiswa dengan NIM '" + searchNim + "' tidak ditemukan.");
                        }
                        break;

                    case "3":
                        System.out.print("Masukkan kata kunci nama: ");
                        String searchNama = scanner.nextLine().trim();
                        List<Mahasiswa> listNama = service.cariMahasiswaByNama(searchNama);
                        System.out.println("\n--- HASIL PENCARIAN NAMA '" + searchNama + "' (Ditemukan: " + listNama.size() + ") ---");
                        for (Mahasiswa m : listNama) {
                            System.out.printf(" - NIM: %-10s | Nama: %-25s | Jurusan: %s\n",
                                    m.getNim(), m.getNama(), m.getJurusan());
                        }
                        break;

                    case "4":
                        System.out.print("Masukkan NIM baru: ");
                        String nimBaru = scanner.nextLine().trim();
                        System.out.print("Masukkan Nama: ");
                        String namaBaru = scanner.nextLine().trim();
                        System.out.print("Masukkan Jurusan: ");
                        String jurBaru = scanner.nextLine().trim();

                        boolean added = service.tambahMahasiswa(new Mahasiswa(nimBaru, namaBaru, jurBaru));
                        if (added) {
                            System.out.println("✔ Sukses menambahkan mahasiswa baru.");
                        } else {
                            System.out.println("❌ Gagal! NIM sudah terdaftar atau input tidak valid.");
                        }
                        break;

                    case "5":
                        System.out.print("Masukkan NIM yang ingin diupdate: ");
                        String updateNim = scanner.nextLine().trim();
                        Mahasiswa existing = service.getMahasiswaByNim(updateNim);
                        if (existing == null) {
                            System.out.println("❌ Mahasiswa dengan NIM '" + updateNim + "' tidak ditemukan.");
                            break;
                        }
                        System.out.print("Masukkan Nama baru (sebelumnya: " + existing.getNama() + "): ");
                        String updNama = scanner.nextLine().trim();
                        System.out.print("Masukkan Jurusan baru (sebelumnya: " + existing.getJurusan() + "): ");
                        String updJur = scanner.nextLine().trim();

                        boolean updated = service.updateMahasiswa(new Mahasiswa(updateNim, updNama, updJur));
                        if (updated) {
                            System.out.println("✔ Sukses memperbarui data mahasiswa.");
                        } else {
                            System.out.println("❌ Gagal memperbarui data.");
                        }
                        break;

                    case "6":
                        System.out.print("Masukkan NIM yang ingin dihapus: ");
                        String hapusNim = scanner.nextLine().trim();
                        boolean deleted = service.hapusMahasiswa(hapusNim);
                        if (deleted) {
                            System.out.println("✔ Sukses menghapus mahasiswa dengan NIM " + hapusNim);
                        } else {
                            System.out.println("❌ Gagal! Mahasiswa dengan NIM " + hapusNim + " tidak ditemukan.");
                        }
                        break;

                    case "7":
                        int total = service.getJumlahMahasiswa();
                        System.out.println("✔ Total jumlah mahasiswa terdaftar di server: " + total);
                        break;

                    case "0":
                        running = false;
                        System.out.println("Keluar dari program client. Sampai jumpa!");
                        break;

                    default:
                        System.out.println("Pilihan tidak valid, silakan ulangi.");
                }
            } catch (ConnectException e) {
                System.err.println("❌ KONEKSI TERPUTUS: RMI Server tiba-tiba offline!");
            } catch (Exception e) {
                System.err.println("❌ Terjadi error saat memanggil remote method: " + e.getMessage());
            }
        }
        scanner.close();
    }
}
