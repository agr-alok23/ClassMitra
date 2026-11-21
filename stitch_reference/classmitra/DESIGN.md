---
name: ClassMitra
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#434655'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#4b41e1'
  on-secondary: '#ffffff'
  secondary-container: '#645efb'
  on-secondary-container: '#fffbff'
  tertiary: '#006242'
  on-tertiary: '#ffffff'
  tertiary-container: '#007d55'
  on-tertiary-container: '#bdffdb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#e2dfff'
  secondary-fixed-dim: '#c3c0ff'
  on-secondary-fixed: '#0f0069'
  on-secondary-fixed-variant: '#3323cc'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-sm:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.15px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  margin-mobile: 16px
  margin-desktop: 32px
  gutter: 16px
---

## Brand & Style

This design system is built on the principles of **Modern Minimalism** with a heavy influence from **Material Design 3**. It is crafted to feel like a premium, native Android experience that prioritizes clarity, focus, and ease of use for both educators and learners. 

The brand personality is **intelligent, supportive, and dependable**. The UI evokes an emotional response of "effortless productivity"—reducing cognitive load through generous whitespace, a structured hierarchy, and soft, approachable aesthetics. By utilizing a "Systematic Utility" approach, the interface disappears to let the educational content and AI insights take center stage.

## Colors

The color strategy utilizes a **Refined Professional** palette. 
- **Primary (Blue):** Used for key actions, active states, and brand recognition.
- **Secondary (Indigo):** Reserved for supplemental interactive elements and AI-driven features to distinguish them from standard tasks.
- **Accent (Emerald):** Denotes progress, completion, and positive feedback loops.
- **Surface & Background:** A crisp white surface sits atop a slightly cooler neutral background to create a subtle "layered" effect, essential for the Material 3 depth model.

Contrast ratios must strictly adhere to WCAG AA standards to ensure readability for all students, particularly in varied lighting conditions found in classrooms.

## Typography

The design system employs **Inter** for its exceptional legibility on mobile screens and its neutral, systematic character. 

- **Headlines:** Use Semi-Bold weights to provide a strong anchor for page sections.
- **Body Text:** Uses a standard weight with slightly increased line-height (1.5x) to facilitate long-form reading of educational materials.
- **Labels:** Used for navigation items, small buttons, and metadata.
- **Scale:** On mobile devices, `headline-lg` downscales to 28px to prevent awkward text wrapping and maintain a tight visual rhythm.

## Layout & Spacing

This design system follows a **Mobile-First Fluid Grid** model. 

- **Mobile:** A 4-column layout with 16px margins and 16px gutters.
- **Tablet/Desktop:** A 12-column grid that centers the content with a max-width of 1200px.
- **Rhythm:** An 8px linear scale is used for all layout adjustments, while a 4px scale is reserved for tight internal component spacing (e.g., icon-to-text).

Layout transitions should be smooth; as screen real estate increases, cards should expand horizontally until they hit their maximum readable width, at which point they should transition into a multi-column masonry or grid view.

## Elevation & Depth

Visual hierarchy is established using **Tonal Layers** and **Ambient Shadows** inspired by Material 3. 

1. **Level 0 (Background):** `#F8FAFC` - The canvas.
2. **Level 1 (Cards/Surfaces):** White background with a very soft, diffused shadow (`0px 2px 8px rgba(0, 0, 0, 0.05)`).
3. **Level 2 (Floating/Active):** Higher elevation for elements like FABs (Floating Action Buttons) or active modals, using a more pronounced shadow (`0px 8px 24px rgba(0, 0, 0, 0.08)`).

Avoid hard borders; instead, use subtle tonal differences (`surface-variant`) to define areas like headers or sidebars. This creates a "soft" interface that feels modern and approachable.

## Shapes

The shape language is characterized by **Soft Geometricism**. 
- **Standard Components:** Buttons, Input Fields, and Chips use a `0.5rem (8px)` radius.
- **Container Elements:** Cards and Modals use a `1rem (16px)` radius to create a friendly, "cradled" feel for content.
- **Action Elements:** Floating Action Buttons (FABs) and certain decorative chips may use a full pill-shape (32px+) to signify their interactive priority.

The consistency of the 16px corner for major containers is the cornerstone of the system's "friendly" aesthetic.

## Components

- **Buttons:** 
    - *Primary:* Filled with Primary Blue, white text, 8px corners.
    - *Secondary:* Tonal (Light Blue/Indigo background) with Primary colored text.
    - *AI Action:* Indigo gradient or sparkle icon to denote AI-powered features.
- **Cards:** White surface, 16px rounded corners, subtle shadow. Use a 16px internal padding for all content.
- **Inputs:** Outlined style with a 1px border in a neutral-300 shade. On focus, the border thickens to 2px and changes to Primary Blue.
- **Chips:** Small, pill-shaped elements for tags or filters. Use `label-md` typography.
- **Lists:** Clean, 72dp height for mobile list items with 16px horizontal padding. Use Material Symbols Rounded for all leading icons.
- **Floating Action Button (FAB):** The primary driver for "New Lesson" or "Ask AI." Use a rounded-square (16px) or large pill shape, positioned at the bottom right.
- **Progress Indicators:** Linear or circular bars using the Accent (Emerald) color to signify achievement and growth.