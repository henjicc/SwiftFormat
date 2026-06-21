---
name: Fluid Exchange
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#3c4a46'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#6c7a76'
  outline-variant: '#bbcac4'
  surface-tint: '#006b5c'
  primary: '#006b5c'
  on-primary: '#ffffff'
  primary-container: '#00bfa5'
  on-primary-container: '#00473c'
  inverse-primary: '#44ddc1'
  secondary: '#516161'
  on-secondary: '#ffffff'
  secondary-container: '#d4e6e5'
  on-secondary-container: '#576867'
  tertiary: '#546067'
  on-tertiary: '#ffffff'
  tertiary-container: '#9facb3'
  on-tertiary-container: '#344047'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#68fadd'
  primary-fixed-dim: '#44ddc1'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#005145'
  secondary-fixed: '#d4e6e5'
  secondary-fixed-dim: '#b8cac9'
  on-secondary-fixed: '#0e1e1e'
  on-secondary-fixed-variant: '#3a4a49'
  tertiary-fixed: '#d7e4ec'
  tertiary-fixed-dim: '#bbc8d0'
  on-tertiary-fixed: '#111d23'
  on-tertiary-fixed-variant: '#3c494f'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Be Vietnam Pro
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Be Vietnam Pro
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  baseline: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  margin-mobile: 16px
  gutter-mobile: 12px
---

## Brand & Style

The design system is built for a high-utility file conversion utility, prioritizing speed, clarity, and a native Android feel. It leverages **Minimalism** blended with **Material Design 3 (MD3)** principles to create an interface that feels like an extension of the operating system.

The target audience ranges from casual users needing quick image resizing to professionals managing batch audio/video transcodes. The emotional response should be one of **effortless efficiency** and **technical reliability**. The aesthetic uses generous whitespace, a light-filled canvas, and subtle depth to focus the user's attention entirely on the file conversion workflow.

## Colors

The palette is anchored by a "Mint Teale" primary color, chosen for its association with freshness and modern technology. 

- **Primary (#00BFA5):** Used for key actions, progress bars, and active states.
- **Secondary (#E0F2F1):** A soft mint tint used for large surface areas, container backgrounds, and subtle highlights to reduce visual fatigue.
- **Tertiary (#263238):** A deep blue-gray used for high-contrast text and icon elements to ensure legibility.
- **Neutral (#F8F9FA):** The foundation color for the application background, providing a clean "paper" feel.

Functional colors for Error (Red 600), Warning (Amber 500), and Success (Green 600) should follow standard MD3 tonal palettes.

## Typography

This design system utilizes a trio of typefaces to balance personality with utility. **Be Vietnam Pro** provides a contemporary, friendly look for headlines. **Roboto Flex** is the workhorse for body text, ensuring maximum readability and a native Android feel. **JetBrains Mono** is used sparingly for technical metadata (file sizes, formats, bitrates) to give a precise, "pro" tool aesthetic.

For mobile screens, scale headlines down by 15-20% to prevent excessive line-breaking. Maintain a minimum tap-target friendly line height of 1.4x for body copy.

## Layout & Spacing

Following the MD3 8dp grid system, the layout relies on a **Fluid Grid** model. On mobile devices, use a 4-column grid with 16px outer margins and 12px gutters.

- **Vertical Rhythm:** Elements should be spaced in increments of 8px. Use 16px (md) for standard padding within cards and 24px (lg) for section separation.
- **Touch Targets:** All interactive elements must maintain a minimum 48x48dp touch area, regardless of their visual size.
- **Safe Areas:** Ensure content respects the system status bar and home indicator areas, using dynamic padding at the top and bottom of the viewport.

## Elevation & Depth

Visual hierarchy is established through **Tonal Layers** and **Ambient Shadows**. This design system avoids harsh dropshadows in favor of soft, diffused shadows that simulate a natural light source from above.

- **Level 0 (Flat):** The main background (`#F8F9FA`).
- **Level 1 (Surface):** Cards and secondary containers using `#E0F2F1` or white with a 2dp soft shadow (blur: 4px, opacity: 0.08).
- **Level 2 (Active/Floating):** Floating Action Buttons (FABs) and active dialogs use a 6dp shadow (blur: 12px, opacity: 0.12) to appear closer to the user.
- **State Changes:** On press, elements should decrease elevation (simulating a physical push) and trigger a subtle primary-colored ripple effect.

## Shapes

The shape language is characterized by high-radius curves to soften the technical nature of a file converter.

- **Containers/Sheets:** Large bottom sheets and main view containers use a **28dp** top-corner radius to align with MD3 standards.
- **Cards:** Standard file cards and item containers use a **16dp** radius.
- **Buttons:** Small buttons and input fields use a **12dp** radius, while the FAB remains a fully rounded square (28dp radius or pill-shaped).
- **Icons:** Icons should utilize a rounded "capsule" style for their internal strokes to maintain consistency with the UI's roundedness.

## Components

### Buttons & FAB
The **Floating Action Button (FAB)** is the primary trigger for the "Add File" action. It should be a large 56x56dp container using the Primary color with a high-contrast white or dark icon. Standard buttons use the Secondary color for low-priority actions and Primary for "Convert" triggers.

### Cards
File cards should display the file name, an icon representing the format (e.g., a play button for video), and a progress bar during conversion. Cards use the `rounded-lg` (16dp) setting and a Level 1 shadow.

### Input Fields
Fields for "Output Format" or "Resolution" should be "Outlined" style with a 12dp radius. Use the Secondary color for the outline in inactive states, turning Primary when focused.

### Chips
Use chips for filtering file types (All, Images, Video, Audio). Chips are pill-shaped (rounded-xl) and use the Secondary color for the background, with the Primary color indicating the selected state.

### Lists
Lists of converted files should feature a 72dp height per row, with trailing icons for "Download" or "Delete" and leading icons indicating the file category. Use a thin `#E0F2F1` divider between items.