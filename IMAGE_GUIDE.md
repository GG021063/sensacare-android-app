# Image Management Guide for Sensacare PWA

## Overview
This guide covers the best practices for adding and managing images in your PWA application.

## Image Strategies

### 1. **Local SVG Images (Recommended for PWA)**
**Best for:** Icons, illustrations, simple graphics
**Pros:** 
- Scalable, lightweight, fast loading
- No external dependencies
- Perfect for PWA offline functionality
- Can be animated and styled with CSS

**File Structure:**
```
public/
├── images/
│   ├── wellbeing/
│   │   ├── mindfulness.svg
│   │   ├── sleep.svg
│   │   ├── stress-relief.svg
│   │   └── exercise.svg
│   ├── vitals/
│   │   ├── heart-rate.svg
│   │   ├── blood-pressure.svg
│   │   └── temperature.svg
│   └── icons/
│       ├── device.svg
│       └── settings.svg
```

**Usage:**
```jsx
// In React components
<img src="/images/wellbeing/mindfulness.svg" alt="Mindfulness" />
// or as background
<div style={{ backgroundImage: 'url(/images/wellbeing/mindfulness.svg)' }} />
```

### 2. **Local PNG/JPG Images**
**Best for:** Photos, complex graphics, logos
**Pros:** 
- High quality, detailed images
- Good browser support
- Can be optimized for web

**File Structure:**
```
public/
├── images/
│   ├── photos/
│   │   ├── user-avatar.jpg
│   │   └── device-photo.png
│   └── logos/
│       └── sensacare-logo.png
```

**Usage:**
```jsx
<img src="/images/photos/user-avatar.jpg" alt="User Avatar" />
```

### 3. **Base64 Encoded Images**
**Best for:** Small icons, inline images
**Pros:** 
- No additional HTTP requests
- Works offline immediately
- Good for very small images

**Usage:**
```jsx
<img src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTAwIiBoZWlnaHQ9IjMwMCI..." alt="Icon" />
```

### 4. **External CDN Images (Use Sparingly)**
**Best for:** Large photos, user-generated content
**Pros:** 
- Don't bloat your app bundle
- Can use CDN optimization
- Good for dynamic content

**Cons:**
- Requires internet connection
- External dependency
- Potential loading delays

**Usage:**
```jsx
<img src="https://cdn.example.com/image.jpg" alt="External Image" />
```

## Image Optimization Best Practices

### 1. **SVG Optimization**
- Use tools like SVGO to compress SVGs
- Remove unnecessary metadata
- Optimize paths and shapes

### 2. **PNG/JPG Optimization**
- Use WebP format when possible
- Compress images appropriately
- Use responsive images with `srcset`

### 3. **Lazy Loading**
```jsx
<img src="/image.jpg" loading="lazy" alt="Description" />
```

### 4. **Responsive Images**
```jsx
<img 
  src="/image-small.jpg"
  srcSet="/image-small.jpg 300w, /image-medium.jpg 600w, /image-large.jpg 900w"
  sizes="(max-width: 600px) 300px, (max-width: 900px) 600px, 900px"
  alt="Responsive Image"
/>
```

## PWA-Specific Considerations

### 1. **Service Worker Caching**
Images in the `public/` folder are automatically cached by the service worker.

### 2. **Offline Functionality**
- Use local images for critical UI elements
- Provide fallbacks for external images
- Consider using placeholder SVGs

### 3. **App Bundle Size**
- Keep images under 1MB total for fast loading
- Use SVGs for icons and simple graphics
- Optimize PNG/JPG files

## Creating Custom SVG Images

### 1. **Using Online Tools**
- **Figma:** Design and export SVGs
- **Inkscape:** Free SVG editor
- **Adobe Illustrator:** Professional SVG creation

### 2. **SVG Structure Example**
```svg
<svg width="500" height="300" viewBox="0 0 500 300" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#fbbf24;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#f59e0b;stop-opacity:1" />
    </linearGradient>
  </defs>
  
  <!-- Background -->
  <rect width="500" height="300" fill="url(#gradient)"/>
  
  <!-- Content -->
  <circle cx="250" cy="150" r="50" fill="white"/>
  
  <!-- Text -->
  <text x="250" y="250" font-family="Arial, sans-serif" font-size="18" fill="white" text-anchor="middle">Title</text>
</svg>
```

## Adding Images to Your App

### Step 1: Create the Image
1. Design your image (SVG recommended)
2. Save it in the appropriate folder under `public/images/`

### Step 2: Import and Use
```jsx
// In your React component
const MyComponent = () => {
  return (
    <div className="image-container">
      <img 
        src="/images/wellbeing/mindfulness.svg" 
        alt="Mindfulness meditation"
        className="w-full h-auto"
      />
    </div>
  );
};
```

### Step 3: Style with CSS
```css
.image-container img {
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: transform 0.2s ease;
}

.image-container img:hover {
  transform: scale(1.05);
}
```

## Image Categories for Sensacare

### 1. **Wellbeing Images**
- Meditation and mindfulness
- Sleep and relaxation
- Exercise and fitness
- Stress relief techniques

### 2. **Vitals Images**
- Heart rate graphics
- Blood pressure indicators
- Temperature scales
- Oxygen saturation displays

### 3. **Device Images**
- Wearable device illustrations
- Connection status icons
- Calibration graphics

### 4. **UI Icons**
- Navigation icons
- Action buttons
- Status indicators
- Settings icons

## Performance Tips

1. **Use SVGs for icons and simple graphics**
2. **Optimize PNG/JPG files before adding**
3. **Implement lazy loading for large images**
4. **Use appropriate image sizes**
5. **Cache images in service worker**
6. **Provide alt text for accessibility**

## Tools and Resources

### Image Creation
- **Figma:** https://figma.com
- **Inkscape:** https://inkscape.org
- **Canva:** https://canva.com

### Image Optimization
- **TinyPNG:** https://tinypng.com
- **SVGO:** https://github.com/svg/svgo
- **Squoosh:** https://squoosh.app

### Icon Libraries
- **Heroicons:** https://heroicons.com
- **Feather Icons:** https://feathericons.com
- **Lucide Icons:** https://lucide.dev 