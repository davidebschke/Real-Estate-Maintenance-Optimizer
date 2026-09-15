/** A selectable property with example master data, shown as the "Objekt" dropdown until a real properties backend exists. */
export interface ExampleProperty {
  id: string
  name: string
  address: string
}

/** Hardcoded example properties used to populate the appointment form's "Objekt" dropdown. */
export const exampleProperties: ExampleProperty[] = [
  { id: '1', name: 'Wohnanlage Sonnenhof', address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld' },
  { id: '2', name: 'Wohnpark Lindenthal', address: 'Dürener Str. 220, 50931 Köln' },
  { id: '3', name: 'Wohnanlage Rheinblick', address: 'Rheinuferstr. 8, 50996 Köln-Rodenkirchen' },
]
