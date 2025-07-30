# Sensacare PWA

A Progressive Web App for health monitoring and wellness tracking.

## Development

```bash
npm install
npm run dev
```

## Deployment

This project is configured for automatic deployment on Vercel. Every push to the main branch will trigger a new deployment.

### Vercel Configuration

The project includes a `vercel.json` file with the following settings:
- **Framework**: Vite
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm install`

### Automatic Deployments

1. **Git Integration**: The project is connected to Vercel and will automatically deploy on every push to the main branch
2. **Preview Deployments**: Pull requests will create preview deployments
3. **Production Deployments**: Merges to main branch deploy to production

### Manual Deployment

If you need to deploy manually:

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Deploy to production
vercel --prod
```

### Environment Variables

Make sure to configure the following environment variables in your Vercel project settings:

- `VITE_SUPABASE_URL` - Your Supabase project URL
- `VITE_SUPABASE_ANON_KEY` - Your Supabase anonymous key
- `VITE_USE_MOCK_DATA` - Set to 'true' for development, 'false' for production

---

## Local Setup

1. Prerequisites  
   • Node ≥ 18   • npm ≥ 9   • Git   • Supabase project   • Bluetooth-capable browser  
2. Clone & install  
```powershell
git clone https://github.com/GG021063/sensacare-PWA-app.git
cd sensacare-PWA-app
npm install        # or pnpm / yarn
```
3. Create `.env.local`  
```
VITE_SUPABASE_URL=https://<project>.supabase.co
VITE_SUPABASE_ANON_KEY=<your-anon-key>
VITE_USE_MOCK_DATA=true        # enable mock vitals in dev
```
4. Run dev server  
```powershell
npm run dev
```  
   Open `http://localhost:3000` – hot-reload enabled.  
5. Production build  
```powershell
npm run build     # outputs to /dist
npm run preview   # local preview of prod bundle
```

---

## Development Workflow

```powershell
# after making code changes
git status                 # view changed files
git add .                  # stage changes
git commit -m "feat: short description"
git push                   # push to GitHub (main or feature branch)
```

*Create feature branches:*  
```powershell
git checkout -b feature/your-topic
# work, commit, push
git push -u origin feature/your-topic
# open PR → Vercel preview → review & merge
```

---

## Bluetooth Mock Mode

To explore the UI without hardware:  
```
VITE_USE_MOCK_DATA=true
```  
Then choose **Developer Options → Mock Ring/Watch** inside the Connect-Device screen.

---

## Security & Compliance

* Supabase RLS & encrypted storage for PHI  
* HTTP-only cookies for sessions; tokens stored in secure storage  
* GDPR / HIPAA-ready schema with audit fields  
* Optional WebAuthn / biometric re-auth scaffold  

---

## Project Structure (abridged)

```
src/
 ├─ assets/            images & icons
 ├─ context/           Auth, Theme, Device, Notifications
 ├─ hooks/             BLE, Supabase, mock data
 ├─ layouts/           Main & Auth shells
 ├─ pages/             Dashboard, Sleep, Activity…
 ├─ lib/               device ingestion & helpers
 └─ styles/            Tailwind globals
```
Full tree in `project-structure.md`.

---

## Contributing

1. Fork or create a feature branch off `main`  
2. `npm run lint && npm test` before committing  
3. Open a Pull Request; Vercel auto-previews  
4. After review & CI pass, PR is merged.

---

## License

MIT © 2025 Sensacare
# Trigger deployment




# Force Vercel deployment
