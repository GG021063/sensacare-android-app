# Deployment Checklist for Sensacare PWA

## ✅ Vercel Configuration (COMPLETED)

- [x] `vercel.json` configured with proper build settings
- [x] Framework set to Vite
- [x] Build command: `npm run build`
- [x] Output directory: `dist`
- [x] Install command: `npm install`
- [x] PWA headers configured for manifest.json and service-worker.js

## 🔧 Required Setup Steps

### 1. Vercel Project Connection
- [ ] Ensure project is connected to Vercel dashboard
- [ ] Verify Git repository integration
- [ ] Set up automatic deployments for main branch

### 2. Environment Variables
Configure these in Vercel dashboard → Settings → Environment Variables:

- [ ] `VITE_SUPABASE_URL` - Your Supabase project URL
- [ ] `VITE_SUPABASE_ANON_KEY` - Your Supabase anonymous key  
- [ ] `VITE_USE_MOCK_DATA` - Set to 'false' for production

### 3. Domain Configuration (Optional)
- [ ] Add custom domain if needed
- [ ] Configure SSL certificates
- [ ] Set up redirects if required

## 🚀 Testing Deployment

### 1. Test Build Locally
```bash
npm run build
```
Verify the `dist/` folder is created successfully.

### 2. Test Deployment
```bash
# Install Vercel CLI if not already installed
npm i -g vercel

# Deploy to preview
vercel

# Deploy to production
vercel --prod
```

### 3. Verify Git Integration
- [ ] Push a test commit to main branch
- [ ] Check Vercel dashboard for automatic deployment
- [ ] Verify deployment URL is accessible
- [ ] Test PWA functionality on deployed site

## 🔍 Post-Deployment Verification

### 1. PWA Features
- [ ] App installs correctly
- [ ] Offline functionality works
- [ ] Service worker is registered
- [ ] Manifest.json loads properly

### 2. Core Functionality
- [ ] Authentication works
- [ ] Device connection works (if applicable)
- [ ] Data sync functions properly
- [ ] All pages load without errors

### 3. Performance
- [ ] Lighthouse PWA score > 90
- [ ] Core Web Vitals are good
- [ ] Images load properly
- [ ] No console errors

## 🛠 Troubleshooting

### Common Issues:
1. **Build Failures**: Check `vercel.json` configuration
2. **Environment Variables**: Ensure all required vars are set in Vercel
3. **PWA Issues**: Verify manifest.json and service worker paths
4. **Routing Issues**: Check SPA rewrite rules in vercel.json

### Debug Commands:
```bash
# Check Vercel project status
vercel ls

# View deployment logs
vercel logs

# Pull latest environment variables
vercel env pull
```

## 📝 Notes

- The project is configured for automatic deployments on every push to main
- Preview deployments are created for pull requests
- All static assets are served from the `public/` directory
- PWA features are automatically configured through the build process 