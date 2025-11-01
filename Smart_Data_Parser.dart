import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'dart:convert';
import 'dart:math';

// --- 1. MODEL DATA & KONSTANTA ---

class Student {
  String nama;
  String kelas;
  String jurusan;
  String sekolah;

  Student({required this.nama, required this.kelas, required this.jurusan, required this.sekolah});

  Map<String, dynamic> toJson() => {
        'nama': nama,
        'kelas': kelas,
        'jurusan': jurusan,
        'sekolah': sekolah,
      };

  factory Student.fromJson(Map<String, dynamic> json) => Student(
        nama: json['nama'] as String,
        kelas: json['kelas'] as String,
        jurusan: json['jurusan'] as String,
        sekolah: json['sekolah'] as String,
      );
}

const Color kBackgroundColor = Color(0xFFF1F8E9);
const Color kAirlanggaColor = Color(0xFF2196F3); // Biru Muda
const Color kKesehatanColor = Color(0xFF4CAF50); // Hijau Muda

const String airlangga = 'SMK Airlangga';
const String kesehatan = 'SMK Kesehatan Airlangga';

/// NOTE: pastikan tidak ada leading/trailing space pada nama jurusan
const Map<String, List<String>> kJurusanBySekolah = {
  airlangga: ['PPLG', 'AKL', 'MPLB', 'TJKT', 'DKV'],
  kesehatan: ['FKK', 'AKC', 'TLM', ], // jika ada jurusan overlapping, sesuaikan prioritas
};

const List<String> kKelas = ['10', '11', '12'];

// --- 2. UTAMA (MAIN) ---

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Data Parser Sekolah',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        primarySwatch: Colors.green,
        textTheme: GoogleFonts.poppinsTextTheme(),
        scaffoldBackgroundColor: kBackgroundColor,
      ),
      home: const DataParserScreen(),
    );
  }
}

// --- 3. WIDGET UTAMA (STATEFUL DENGAN TickerProviderStateMixin) ---

class DataParserScreen extends StatefulWidget {
  const DataParserScreen({super.key});

  @override
  State<DataParserScreen> createState() => _DataParserScreenState();
}

class _DataParserScreenState extends State<DataParserScreen> with TickerProviderStateMixin {
  // Data State
  List<Student> _studentData = [];
  final TextEditingController _autoInputController = TextEditingController();
  final TextEditingController _manualNamaController = TextEditingController();
  String? _selectedSekolah;
  String? _selectedJurusan;
  String? _selectedKelas;
  String _searchQuery = '';
  final String _filename = 'data_siswa.txt';
  int _totalStudents = 0;
  int _airlanggaCount = 0;
  int _kesehatanCount = 0;

  // ANIMATION: Controller untuk proses otomatis (Spin)
  late AnimationController _processButtonController;
  bool _isProcessing = false;

  // ANIMATION: Controller untuk Staggered Entrance
  late AnimationController _entranceController;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _fadeAnimation;
  
  // ANIMATION: Controller untuk visual feedback saat refresh data
  late AnimationController _refreshFeedbackController;

  @override
  void initState() {
    super.initState();
    _processButtonController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    );

    _entranceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.3), 
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _entranceController,
      curve: Curves.easeOutCubic, 
    ));
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(_entranceController);

    _refreshFeedbackController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );

    _loadData(); 
  }

  @override
  void dispose() {
    _processButtonController.dispose();
    _entranceController.dispose();
    _refreshFeedbackController.dispose();
    _autoInputController.dispose();
    _manualNamaController.dispose();
    super.dispose();
  }

  // --- 4. LOGIKA BISNIS & FILE HANDLING (FIXED) ---

  Future<String> get _localPath async {
    final directory = await getApplicationDocumentsDirectory();
    return directory.path;
  }

  Future<File> get _localFile async {
    final path = await _localPath;
    return File('$path/$_filename');
  }

  /// Memuat data dari file lokal ke memori. (Handling file tidak ada/kosong)
  Future<void> _loadData() async {
    try {
      final file = await _localFile;
      if (!await file.exists()) {
        // file belum ada -> pastikan kosong
        setState(() {
          _studentData = [];
          _updateStatistics();
        });
        _entranceController.forward(from: 0.0);
        return;
      }

      String contents = await file.readAsString();
      if (contents.trim().isEmpty) {
        setState(() {
          _studentData = [];
          _updateStatistics();
        });
        _entranceController.forward(from: 0.0);
        return;
      }
      
      final List<dynamic> jsonList = jsonDecode(contents);
      final List<Student> loadedData = jsonList
          .map((json) => Student.fromJson(json as Map<String, dynamic>))
          .toList();

      setState(() {
        _studentData = loadedData;
        _updateStatistics();
      });
      
      _entranceController.forward(from: 0.0);
      _refreshFeedbackController.forward(from: 0.0).then((_) {
        _refreshFeedbackController.reverse();
      });
      
    } catch (e) {
      // Tampilkan pesan user-friendly
      _showErrorSnackbar('Gagal memuat data (cek izin file atau format).');
      setState(() {
        _studentData = [];
        _updateStatistics();
      });
      _entranceController.forward(from: 0.0);
      print('Gagal memuat data: $e');
    }
  }

  Future<void> _saveData() async {
    try {
      final file = await _localFile;
      final List<Map<String, dynamic>> jsonList =
          _studentData.map((s) => s.toJson()).toList();

      // Jika tidak ada data, beri peringatan
      if (jsonList.isEmpty) {
        _showErrorSnackbar('Tidak ada data untuk disimpan.');
        return;
      }

      await file.writeAsString(jsonEncode(jsonList));
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Data berhasil disimpan ke lokal!')),
      );
    } on FileSystemException catch (e) {
      _showErrorSnackbar('Gagal menyimpan data: masalah akses file.');
      print('FileSystemException: $e');
    } catch (e) {
      _showErrorSnackbar('Terjadi kesalahan saat menyimpan data.');
      print('Unexpected save error: $e');
    }
  }

  /// Cari sekolah berdasarkan jurusan (case-insensitive, trimming)
  String? _findSekolahForJurusan(String jurusanInput) {
    final jur = jurusanInput.trim().toUpperCase();
    // Cek setiap sekolah; jika ada overlap di dua sekolah, prioritas sesuai urutan map
    for (var entry in kJurusanBySekolah.entries) {
      if (entry.value.map((e) => e.toUpperCase()).contains(jur)) {
        return entry.key;
      }
    }
    return null;
  }

  /// Memproses Input Otomatis (mendukung multi-line -> multiple matches)
  void _processAutomaticInput() async {
    if (_isProcessing) return;
    
    final text = _autoInputController.text.trim();
    if (text.isEmpty) {
      _showErrorSnackbar('Masukkan teks untuk diproses.');
      return;
    }

    setState(() { _isProcessing = true; });
    _processButtonController.repeat();

    // tunggu sejenak agar animasi terlihat
    await Future.delayed(const Duration(milliseconds: 500));

    // Regex: capture "Nama ... kelas 10|11|12 JURUSAN"
    final regex = RegExp(
      r'([\w\s.\-]+?)\s+kelas\s+(10|11|12)\s+([A-Za-z0-9]+)',
      caseSensitive: false,
      multiLine: true,
    );

    final matches = regex.allMatches(text).toList();
    if (matches.isEmpty) {
      _showErrorSnackbar('Format input tidak valid. Gunakan: [Nama] kelas [10/11/12] [Jurusan]');
      _processButtonController.stop();
      setState(() { _isProcessing = false; });
      return;
    }

    int added = 0;
    int unknownJurusan = 0;

    for (final match in matches) {
      final nama = match.group(1)!.trim();
      final kelas = match.group(2)!.trim();
      final jurusan = match.group(3)!.toUpperCase().trim();

      final sekolah = _findSekolahForJurusan(jurusan);

      if (sekolah != null) {
        final newStudent = Student(nama: nama, kelas: kelas, jurusan: jurusan, sekolah: sekolah);
        setState(() {
          _studentData.add(newStudent);
        });
        added++;
      } else {
        unknownJurusan++;
        // jangan break — teruskan untuk yang lain
      }
    }

    // clear input jika ada penambahan
    if (added > 0) _autoInputController.clear();
    _updateStatistics();

    _processButtonController.stop();
    setState(() { _isProcessing = false; });

    if (added > 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Berhasil menambahkan $added siswa.')),
      );
    }
    if (unknownJurusan > 0) {
      _showErrorSnackbar('$unknownJurusan baris memiliki jurusan yang tidak dikenali.');
    }
  }

  /// Menambah data siswa dari Input Manual
  void _addManualInput() {
    if (_manualNamaController.text.isEmpty || _selectedSekolah == null || _selectedJurusan == null || _selectedKelas == null) {
      _showErrorSnackbar('Semua kolom manual harus diisi.');
      return;
    }

    final newStudent = Student(
      nama: _manualNamaController.text.trim(),
      kelas: _selectedKelas!,
      jurusan: _selectedJurusan!,
      sekolah: _selectedSekolah!,
    );

    setState(() {
      _studentData.add(newStudent);
      _manualNamaController.clear();
      _selectedSekolah = null;
      _selectedJurusan = null;
      _selectedKelas = null;
      _updateStatistics();
    });
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Siswa ${newStudent.nama} berhasil ditambahkan secara manual!')),
    );
  }

  void _clearAllData() async {
    bool confirm = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Konfirmasi Hapus'),
        content: const Text('Yakin ingin menghapus SEMUA data siswa?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Batal')),
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('Hapus', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirm) {
      setState(() {
        _studentData.clear();
        _updateStatistics();
      });
      try {
        final file = await _localFile;
        if (await file.exists()) {
          await file.delete();
        }
      } catch (e) {
        print('Gagal menghapus file lokal: $e');
      }

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Semua data berhasil dihapus!')),
      );
    }
  }

  void _updateStatistics() {
    _totalStudents = _studentData.length;
    _airlanggaCount = _studentData.where((s) => s.sekolah == airlangga).length;
    _kesehatanCount = _studentData.where((s) => s.sekolah == kesehatan).length;
  }

  // _getGroupedData() akan dipanggil di dalam build(), jadi otomatis terpanggil saat setState
  Map<String, Map<String, Map<String, List<Student>>>> _getGroupedData() {
    final grouped = <String, Map<String, Map<String, List<Student>>>>{};
    final filteredData = _studentData.where((s) =>
        s.nama.toLowerCase().contains(_searchQuery.toLowerCase()) ||
        s.jurusan.toLowerCase().contains(_searchQuery.toLowerCase()));

    for (var student in filteredData) {
      grouped.putIfAbsent(student.sekolah, () => {});
      grouped[student.sekolah]!.putIfAbsent(student.jurusan, () => {});
      grouped[student.sekolah]![student.jurusan]!.putIfAbsent(student.kelas, () => []);
      grouped[student.sekolah]![student.jurusan]![student.kelas]!.add(student);
    }
    return grouped;
  }

  // --- 5. UI WIDGETS DENGAN ANIMASI ---

  @override
  Widget build(BuildContext context) {
    final groupedData = _getGroupedData();

    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Smart Data Parser Sekolah',
          style: GoogleFonts.poppins(fontWeight: FontWeight.bold),
        ),
        backgroundColor: kBackgroundColor,
        elevation: 0,
      ),
      body: FadeTransition( 
        opacity: _fadeAnimation,
        child: SlideTransition(
          position: _slideAnimation,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                _buildAutoInputCard(),
                const SizedBox(height: 16),
                _buildManualInputCard(),
                const SizedBox(height: 16),
                _buildActionButtons(),
                const SizedBox(height: 16),
                _buildSearchInput(),
                AnimatedBuilder(
                  animation: _refreshFeedbackController,
                  builder: (context, child) {
                    final double opacity = 1.0 - _refreshFeedbackController.value;
                    return Opacity(
                      opacity: opacity.clamp(0.5, 1.0),
                      child: child,
                    );
                  },
                  child: _buildHierarchyTable(groupedData),
                ),
                const SizedBox(height: 16),
                _buildStatisticsLabel(),
              ],
            ),
          ),
        ),
      ),
      floatingActionButton: _buildProcessFAB(),
    );
  }
  
  Widget _buildProcessFAB() {
    return AnimatedBuilder(
      animation: _processButtonController,
      builder: (context, child) {
        return FloatingActionButton.extended(
          onPressed: _processAutomaticInput,
          backgroundColor: kAirlanggaColor,
          foregroundColor: Colors.white,
          icon: _isProcessing
              ? Transform.rotate(
                  angle: _processButtonController.value * 2.0 * pi,
                  child: const Icon(Icons.cached, color: Colors.white),
                )
              : const Icon(Icons.settings),
          label: _isProcessing
              ? const Text('Memproses...')
              : const Text('Proses Otomatis'),
        );
      },
    );
  }

  Widget _buildAutoInputCard() {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Input Otomatis (boleh multi-line)',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(
              controller: _autoInputController,
              minLines: 1,
              maxLines: 8,
              decoration: const InputDecoration(
                hintText: 'Contoh:\nRina Safitri kelas 10 PPLG\nBudi kelas 11 MPLB',
                border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10))),
                isDense: true,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildManualInputCard() {
    List<String> availableJurusan = _selectedSekolah != null && kJurusanBySekolah.containsKey(_selectedSekolah)
        ? kJurusanBySekolah[_selectedSekolah]!
        : [];

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Input Manual', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(
              controller: _manualNamaController,
              decoration: const InputDecoration(labelText: 'Nama Siswa', border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10))), isDense: true),
            ),
            const SizedBox(height: 10),
            DropdownButtonFormField<String>(
              value: _selectedSekolah,
              decoration: const InputDecoration(labelText: 'Sekolah', border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10))), isDense: true),
              items: [airlangga, kesehatan].map((String value) => DropdownMenuItem<String>(value: value, child: Text(value))).toList(),
              onChanged: (String? newValue) {
                setState(() {
                  _selectedSekolah = newValue;
                  _selectedJurusan = null;
                });
              },
            ),
            const SizedBox(height: 10),
            DropdownButtonFormField<String>(
              value: _selectedJurusan,
              decoration: const InputDecoration(labelText: 'Jurusan', border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10))), isDense: true),
              items: availableJurusan.map((String value) => DropdownMenuItem<String>(value: value, child: Text(value))).toList(),
              onChanged: availableJurusan.isEmpty ? null : (String? newValue) {
                      setState(() {
                        _selectedJurusan = newValue;
                      });
                    },
            ),
            const SizedBox(height: 10),
            DropdownButtonFormField<String>(
              value: _selectedKelas,
              decoration: const InputDecoration(labelText: 'Kelas', border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(10))), isDense: true),
              items: kKelas.map((String value) => DropdownMenuItem<String>(value: value, child: Text(value))).toList(),
              onChanged: (String? newValue) {
                setState(() {
                  _selectedKelas = newValue;
                });
              },
            ),
            const SizedBox(height: 10),
            ElevatedButton.icon(
              onPressed: _addManualInput,
              icon: const Icon(Icons.add),
              label: const Text('Tambah Manual'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size.fromHeight(40),
                backgroundColor: kKesehatanColor,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionButton(IconData icon, String label, VoidCallback onPressed, Color color) {
    return ElevatedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon),
      label: Text(label),
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        padding: const EdgeInsets.symmetric(vertical: 10),
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: [
        Expanded(child: _buildActionButton(Icons.save, 'Simpan', _saveData, Colors.indigo)),
        const SizedBox(width: 8),
        Expanded(child: _buildActionButton(Icons.refresh, 'Segarkan', _loadData, Colors.orange)),
        const SizedBox(width: 8),
        Expanded(child: _buildActionButton(Icons.delete_forever, 'Hapus', _clearAllData, Colors.red)),
        const SizedBox(width: 8),
        // Tombol uji coba: muat 40 data
        SizedBox(
          width: 150,
          child: _buildActionButton(Icons.bolt, 'Uji 40 Data', _loadDummyData, Colors.purple),
        ),
      ],
    );
  }

  Widget _buildSearchInput() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: TextField(
        decoration: InputDecoration(
          labelText: 'Cari Siswa/Jurusan',
          prefixIcon: const Icon(Icons.search),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
          isDense: true,
        ),
        onChanged: (value) {
          setState(() { 
            _searchQuery = value;
          });
        },
      ),
    );
  }

  Widget _buildStatisticsLabel() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Text(
        '👥 Total siswa: $_totalStudents '
        '(${airlangga}: $_airlanggaCount | '
        '${kesehatan}: $_kesehatanCount)',
        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
      ),
    );
  }

  Widget _buildHierarchyTable(Map<String, Map<String, Map<String, List<Student>>>> groupedData) {
    if (_studentData.isEmpty && _searchQuery.isEmpty) {
      return const Center(child: Text('Belum ada data siswa. Silakan masukkan data.'));
    }

    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: groupedData.keys.length,
          itemBuilder: (context, index) {
            final sekolah = groupedData.keys.elementAt(index);
            final jurusanData = groupedData[sekolah]!;
            final isAirlangga = sekolah == airlangga;
            final schoolColor = isAirlangga ? kAirlanggaColor.withOpacity(0.1) : kKesehatanColor.withOpacity(0.1);
            final schoolAccentColor = isAirlangga ? kAirlanggaColor : kKesehatanColor;

            return AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              curve: Curves.easeInOut,
              margin: const EdgeInsets.only(bottom: 1.0),
              decoration: BoxDecoration(
                color: schoolColor,
                border: Border(
                  left: BorderSide(color: schoolAccentColor, width: 4),
                ),
              ),
              child: ExpansionTile(
                key: PageStorageKey<String>(sekolah),
                title: Text(
                  sekolah,
                  style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 16, color: schoolAccentColor),
                ),
                tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                children: [
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 200),
                    transitionBuilder: (Widget child, Animation<double> animation) {
                      return FadeTransition(opacity: animation, child: child);
                    },
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      key: ValueKey<int>(jurusanData.length),
                      children: jurusanData.entries.map((entryJurusan) {
                        final jurusan = entryJurusan.key;
                        final kelasData = entryJurusan.value;

                        return Padding(
                          padding: const EdgeInsets.only(left: 8.0, top: 4, bottom: 4),
                          child: ExpansionTile(
                            key: PageStorageKey<String>('$sekolah-$jurusan'),
                            title: Text('├── $jurusan (${_countStudentsInJurusan(kelasData)} Siswa)', style: const TextStyle(fontWeight: FontWeight.w600)),
                            children: kelasData.entries.map((entryKelas) {
                              final kelas = entryKelas.key;
                              final students = entryKelas.value;

                              return Padding(
                                padding: const EdgeInsets.only(left: 8.0),
                                child: ExpansionTile(
                                  key: PageStorageKey<String>('$sekolah-$jurusan-$kelas'),
                                  title: Text('├── Kelas $kelas (${students.length} Siswa)', style: const TextStyle(fontWeight: FontWeight.w500)),
                                  children: students.map((student) {
                                    return ListTile(
                                      leading: Icon(Icons.person, size: 16, color: schoolAccentColor),
                                      title: Text('├── ${student.nama}', style: TextStyle(color: Colors.grey[700])),
                                      contentPadding: const EdgeInsets.only(left: 50, right: 16),
                                      dense: true,
                                    );
                                  }).toList(),
                                ),
                              );
                            }).toList(),
                          ),
                        );
                      }).toList(),
                    ),
                  )
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  int _countStudentsInJurusan(Map<String, List<Student>> kelasData) {
    return kelasData.values.fold(0, (sum, list) => sum + list.length);
  }
  
  void _showErrorSnackbar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.redAccent, behavior: SnackBarBehavior.floating),
    );
  }

  // --- Fungsi uji coba: muat 40 data contoh sekaligus ---
  void _loadDummyData() {
    const dummyInputs = [
      "Rina Safitri kelas 10 PPLG",
      "Budi Santoso kelas 11 MPLB",
      "Salsa Nur Aini kelas 12 ALL",
      "Reza Maulana kelas 11 DKV",
      "Dewi Lestari kelas 10 FKK",
      "Riko Pratama kelas 12 ALC",
      "Andi Ramadhan kelas 11 TPM",
      "Nisa Amalia kelas 10 PPLG",
      "Yoga Saputra kelas 11 MPLB",
      "Citra Ayu kelas 12 ALL",
      "Rafli Hidayat kelas 10 DKV",
      "Siti Rahma kelas 11 FKK",
      "Fajar Nugraha kelas 12 ALC",
      "Lutfi Ahmad kelas 10 TPM",
      "Ayu Puspita kelas 11 PPLG",
      "Hendra Wijaya kelas 12 MPLB",
      "Fitri Handayani kelas 10 ALL",
      "Ilham Firmansyah kelas 11 DKV",
      "Maya Oktaviani kelas 12 FKK",
      "Rio Alamsyah kelas 10 ALC",
      "Putri Nabila kelas 11 TPM",
      "Yudi Kurniawan kelas 12 PPLG",
      "Laila Kusuma kelas 10 MPLB",
      "Ardiansyah kelas 11 ALL",
      "Melati Cahya kelas 12 DKV",
      "Bagus Prasetyo kelas 10 FKK",
      "Dian Rahayu kelas 11 ALC",
      "Rizky Pramana kelas 12 TPM",
      "Nadya Anjani kelas 10 PPLG",
      "Galih Saputra kelas 11 MPLB",
      "Sarah Amira kelas 12 ALL",
      "Eko Setiawan kelas 10 DKV",
      "Nurul Azizah kelas 11 FKK",
      "Agus Salim kelas 12 ALC",
      "Fina Kartika kelas 10 TPM",
      "Bayu Aditya kelas 11 PPLG",
      "Yulianti Sari kelas 12 MPLB",
      "Hanif Ramdani kelas 10 ALL",
      "Alya Putri kelas 11 DKV",
      "Iqbal Maulana kelas 12 FKK",
    ];

    int added = 0;
    for (var input in dummyInputs) {
      final regex = RegExp(r'([\w\s.\-]+?)\s+kelas\s+(10|11|12)\s+([A-Za-z0-9]+)', caseSensitive: false);
      final match = regex.firstMatch(input);
      if (match != null) {
        final nama = match.group(1)!.trim();
        final kelas = match.group(2)!.trim();
        final jurusan = match.group(3)!.toUpperCase().trim();
        final sekolah = _findSekolahForJurusan(jurusan);
        if (sekolah != null) {
          _studentData.add(Student(nama: nama, kelas: kelas, jurusan: jurusan, sekolah: sekolah));
          added++;
        }
      }
    }

    setState(() {
      _updateStatistics();
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('✅ $added data uji coba berhasil dimuat!')),
    );
  }
}
