package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexWebSplitScalabilityTest {
    private object Refs {
        val Outer =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.outer",
            )

        val OuterHandle =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.outer.handle",
            )

        val LeftColumn =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.left-column",
            )

        val LeftHandle =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.left-column.handle",
            )

        val RightColumn =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.right-column",
            )

        val RightHandle =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.right-column.handle",
            )

        val PlayerOne =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.one",
            )

        val PlayerTwo =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.two",
            )

        val PlayerThree =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.three",
            )

        val PlayerFour =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.four",
            )

        val Overlay =
            VexWeb.StructuralRef.of(
                "vexweb.test.players.overlay",
            )
    }

    @Test
    fun fourPaneLayoutUsesRecursiveSplitsInsteadOfANewContainerVocabulary() {
        val surface =
            vexWebSurface(
                VexLifeWorkspaceBlueprint,
            ) {
                content {
                    split(
                        ref =
                            Refs.Outer,
                        axis =
                            VexWeb.Parameter.SplitAxis.Horizontal,
                        mode =
                            VexWeb.Parameter.SplitMode.Balanced(),
                    ) {
                        leading {
                            split(
                                ref =
                                    Refs.LeftColumn,
                                axis =
                                    VexWeb.Parameter.SplitAxis.Vertical,
                                mode =
                                    VexWeb.Parameter.SplitMode.Balanced(),
                            ) {
                                leading {
                                    structural(
                                        ref =
                                            Refs.PlayerOne,
                                        placement =
                                            VexWeb.Placement.Leading,
                                        depth =
                                            VexWeb.Depth.Content,
                                        presentation =
                                            VexWeb.Presentation.Surface,
                                    )
                                }

                                handle {
                                    structural(
                                        ref =
                                            Refs.LeftHandle,
                                        placement =
                                            VexWeb.Placement.InFlow,
                                        depth =
                                            VexWeb.Depth.Chrome,
                                        presentation =
                                            VexWeb.Presentation.SplitHandle,
                                    )
                                }

                                trailing {
                                    structural(
                                        ref =
                                            Refs.PlayerTwo,
                                        placement =
                                            VexWeb.Placement.Trailing,
                                        depth =
                                            VexWeb.Depth.Content,
                                        presentation =
                                            VexWeb.Presentation.Surface,
                                    )
                                }
                            }
                        }

                        handle {
                            structural(
                                ref =
                                    Refs.OuterHandle,
                                placement =
                                    VexWeb.Placement.InFlow,
                                depth =
                                    VexWeb.Depth.Chrome,
                                presentation =
                                    VexWeb.Presentation.SplitHandle,
                            )
                        }

                        trailing {
                            split(
                                ref =
                                    Refs.RightColumn,
                                axis =
                                    VexWeb.Parameter.SplitAxis.Vertical,
                                mode =
                                    VexWeb.Parameter.SplitMode.Balanced(),
                            ) {
                                leading {
                                    structural(
                                        ref =
                                            Refs.PlayerThree,
                                        placement =
                                            VexWeb.Placement.Leading,
                                        depth =
                                            VexWeb.Depth.Content,
                                        presentation =
                                            VexWeb.Presentation.Surface,
                                    )
                                }

                                handle {
                                    structural(
                                        ref =
                                            Refs.RightHandle,
                                        placement =
                                            VexWeb.Placement.InFlow,
                                        depth =
                                            VexWeb.Depth.Chrome,
                                        presentation =
                                            VexWeb.Presentation.SplitHandle,
                                    )
                                }

                                trailing {
                                    structural(
                                        ref =
                                            Refs.PlayerFour,
                                        placement =
                                            VexWeb.Placement.Trailing,
                                        depth =
                                            VexWeb.Depth.Content,
                                        presentation =
                                            VexWeb.Presentation.Surface,
                                    )
                                }
                            }
                        }
                    }
                }

                overlayRoot {
                    structural(
                        ref =
                            Refs.Overlay,
                        placement =
                            VexWeb.Placement.Overlay,
                        depth =
                            VexWeb.Depth.Transient,
                        presentation =
                            VexWeb.Presentation.Surface,
                    )
                }
            }

        val outer =
            assertIs<
                VexWeb.Split
            >(
                surface.requireNode(
                    Refs.Outer,
                ),
            )
        val left =
            assertIs<
                VexWeb.Split
            >(
                outer.leading,
            )
        val right =
            assertIs<
                VexWeb.Split
            >(
                outer.trailing,
            )

        assertEquals(
            VexWeb.Parameter.SplitAxis.Horizontal,
            outer.axis,
        )
        assertEquals(
            VexWeb.Parameter.SplitAxis.Vertical,
            left.axis,
        )
        assertEquals(
            VexWeb.Parameter.SplitAxis.Vertical,
            right.axis,
        )

        assertEquals(
            listOf(
                Refs.PlayerOne.value,
                Refs.PlayerTwo.value,
                Refs.PlayerThree.value,
                Refs.PlayerFour.value,
            ),
            listOf(
                left.leading.identity.ref,
                left.trailing.identity.ref,
                right.leading.identity.ref,
                right.trailing.identity.ref,
            ),
        )
    }
}

// [VXG RealForever]
