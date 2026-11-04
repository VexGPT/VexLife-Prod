// Bind the hand-authored DOM to accepted Blueprint identity and VexWeb spatial source.
//
// Markup names Kotlin handles (data-vex="Send"). This module resolves each handle through the
// generated manifest and stamps data-vex-ref with the accepted ref. An unknown handle is a visible
// failure, never a silent fallback. Spatial refs (frame, seam, regions, handle, overlay) are found by
// relationship in the manifest, not by retyped names.
//
// [VXG RealForever]

export async function bindWorkspace(root, { fetchImpl = fetch } = {}) {
  const response = await fetchImpl('/generated/workspace-manifest.json');
  if (!response.ok) throw new Error(`workspace manifest unavailable (${response.status})`);
  const manifest = await response.json();
  if (manifest.schemaVersion !== 'vexlife-prod.exploration.web-workspace-manifest/v1') {
    throw new Error(`unexpected manifest schema ${manifest.schemaVersion}`);
  }

  const byHandle = new Map(manifest.elements.map((element) => [element.handle, element]));
  const byRef = new Map(manifest.elements.map((element) => [element.ref, element]));
  const bound = new Map();
  const unknown = [];

  root.querySelectorAll('[data-vex]').forEach((node) => {
    const handle = node.getAttribute('data-vex');
    const element = byHandle.get(handle);
    if (!element) {
      unknown.push(handle);
      return;
    }
    if (bound.has(handle)) throw new Error(`Blueprint handle ${handle} is bound twice`);
    node.setAttribute('data-vex-ref', element.ref);
    node.setAttribute('data-vex-kind', element.kind);
    bound.set(handle, node);
  });

  if (unknown.length > 0) {
    throw new Error(`markup names Blueprint handles the Kotlin source doesn't have: ${unknown.join(', ')}`);
  }

  const screen = root.matches('[data-vex-screen]') ? root : root.querySelector('[data-vex-screen]');
  screen?.setAttribute('data-vex-ref', manifest.screenRef);

  const { coordinates, regions, seams } = manifest.surface;
  const seam = seams.find((candidate) => candidate.axis === 'Vertical');
  if (!seam) throw new Error('accepted spatial source has no vertical seam for the workspace frame');
  const handleCoordinate = coordinates.find((coordinate) => coordinate.presentation === 'SeamHandle'
    && coordinate.binding.type === 'LocalSpatial' && coordinate.binding.target === seam.ref);
  const regionFor = (handle) => regions.find((region) => region.contentRef === byHandle.get(handle)?.ref);
  const snackbarCoordinate = coordinates.find((coordinate) => coordinate.presentation === 'TransientFeedback');
  const overlayCoordinate = coordinates.find((coordinate) => coordinate.placement === 'Overlay'
    && coordinate.presentation === 'Surface' && coordinate.depth === 'Transient');
  const sendCoordinate = coordinates.find((coordinate) => coordinate.ref === byHandle.get('Send')?.ref);

  const stamp = (selector, ref) => {
    const node = root.querySelector(selector);
    if (node && ref) node.setAttribute('data-vex-ref', ref);
  };
  stamp('#frame', seam.frameRef);
  stamp('#seam', handleCoordinate?.ref);
  stamp('#overlay-root', overlayCoordinate?.ref);
  stamp('#snackbar', snackbarCoordinate?.ref);
  root.querySelector('.region-browser')?.setAttribute('data-vex-region', regionFor('Browser')?.ref ?? '');
  root.querySelector('.region-companion')?.setAttribute('data-vex-region', regionFor('Companion')?.ref ?? '');

  return Object.freeze({
    manifest,
    el(handle) {
      const node = bound.get(handle);
      if (!node) throw new Error(`Blueprint handle ${handle} is not present in this renderer`);
      return node;
    },
    refOf(handle) {
      return byHandle.get(handle)?.ref ?? null;
    },
    seam: Object.freeze({
      ref: seam.ref,
      beforeParts: seam.beforeParts,
      afterParts: seam.afterParts,
      interactions: new Set(handleCoordinate?.interactions ?? [])
    }),
    sendSourceBinding: sendCoordinate?.binding ?? null,
    coverage: Object.freeze({
      bound: bound.size,
      declared: manifest.elements.length,
      missing: manifest.elements.filter((element) => !bound.has(element.handle)).map((element) => element.handle),
      parentOf: (handle) => byRef.get(byHandle.get(handle)?.parentRef)?.handle ?? null
    })
  });
}
