const fs = require('fs');
const path = require('path');

const TOKEN = process.env.GITHUB_TOKEN || '';
const REPO = 'contactaurawealth-lab/studyos-exam';
const TAG = 'v1.3.0';
const RELEASE_NAME = 'StudyOS v1.3.0 — Minimal Exam-Focused Edition';
const APK_SOURCE = path.resolve(__dirname, '../docs/downloads/StudyOS.apk');
const RELEASE_NOTES_PATH = path.resolve(__dirname, '../docs/RELEASE_NOTES_v1.3.0.md');

async function main() {
    console.log(`🚀 Publishing GitHub release ${TAG} to ${REPO}...`);

    const body = fs.readFileSync(RELEASE_NOTES_PATH, 'utf8');

    // 1. Create Release
    const createRes = await fetch(`https://api.github.com/repos/${REPO}/releases`, {
        method: 'POST',
        headers: {
            'Authorization': `token ${TOKEN}`,
            'Accept': 'application/vnd.github.v3+json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            tag_name: TAG,
            target_commitish: 'main',
            name: RELEASE_NAME,
            body: body,
            draft: false,
            prerelease: false
        })
    });

    const releaseData = await createRes.json();
    if (!createRes.ok) {
        console.error('❌ Failed to create release:', releaseData);
        process.exit(1);
    }

    console.log(`✅ Created Release ID: ${releaseData.id}`);
    console.log(`🔗 Release URL: ${releaseData.html_url}`);

    const uploadUrlTemplate = releaseData.upload_url; // e.g. https://uploads.github.com/.../assets{?name,label}
    const uploadBase = uploadUrlTemplate.replace(/\{\?name,label\}/, '');

    const apkBuffer = fs.readFileSync(APK_SOURCE);
    console.log(`📦 Read APK (${(apkBuffer.length / (1024 * 1024)).toFixed(2)} MB)`);

    // 2. Upload StudyOS-v1.3.0.apk
    await uploadAsset(uploadBase, 'StudyOS-v1.3.0.apk', apkBuffer);

    // 3. Upload StudyOS.apk
    await uploadAsset(uploadBase, 'StudyOS.apk', apkBuffer);

    console.log('🎉 All release assets successfully published!');
}

async function uploadAsset(uploadBase, assetName, buffer) {
    console.log(`⏳ Uploading ${assetName}...`);
    const res = await fetch(`${uploadBase}?name=${assetName}`, {
        method: 'POST',
        headers: {
            'Authorization': `token ${TOKEN}`,
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Length': buffer.length.toString()
        },
        body: buffer
    });

    const data = await res.json();
    if (!res.ok) {
        console.error(`❌ Failed to upload ${assetName}:`, data);
    } else {
        console.log(`✅ Uploaded ${assetName}: ${data.browser_download_url}`);
    }
}

main().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
