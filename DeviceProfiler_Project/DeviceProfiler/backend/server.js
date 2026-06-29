// -------------------- IMPORTS --------------------
const express = require('express');
const mongoose = require('mongoose');

const app = express();
app.use(express.json());

// -------------------- MONGODB CONNECTION --------------------
mongoose.connect('mongodb://127.0.0.1:27017/deviceProfiler')
    .then(() => console.log("MongoDB Connected"))
    .catch(err => console.log("MongoDB Error:", err));

// -------------------- SCHEMA --------------------
const ProfileSchema = new mongoose.Schema({
    deviceModel: String,
    manufacturer: String,
    androidVersion: String,
    cpuCores: Number,
    totalRamMB: Number,
    batteryPercent: Number,
    isCharging: Boolean,
    benchmarkTimeMs: Number,
    cpuScore: Number,
    memoryScore: Number,
    batteryScore: Number,

    gpuName: String,     // ✅ ADDED
    gpuScore: Number,    // ✅ ADDED

    finalScore: Number,
    grade: String
});

// -------------------- MODEL --------------------
const Profile = mongoose.model("Profile", ProfileSchema);

// -------------------- POST API --------------------
app.post('/profile', async (req, res) => {
    try {
        const data = new Profile(req.body);
        await data.save();

        console.log("Saved to MongoDB:", req.body);

        res.json({
            message: "Data saved successfully",
            id: data._id
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: err.message });
    }
});

// -------------------- GET API --------------------
app.get('/profile', async (req, res) => {
    try {
        const data = await Profile.find();
        res.json(data);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------- DEFAULT ROUTE --------------------
app.get('/', (req, res) => {
    res.send("Device Profiler API is running 🚀");
});

// -------------------- START SERVER --------------------
app.listen(3000, () => {
    console.log("Server running on port 3000");
});