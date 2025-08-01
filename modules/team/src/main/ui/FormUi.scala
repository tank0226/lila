package lila.team
package ui

import play.api.data.{ Field, Form }

import lila.core.captcha.Captcha
import lila.core.team.Access
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class FormUi(helpers: Helpers, bits: TeamUi)(
    renderCaptcha: (Form[?] | Field, Captcha) => Context ?=> Frag
):
  import helpers.{ *, given }
  import trans.team as trt
  import bits.{ TeamPage, menu }

  def create(form: Form[?], captcha: Captcha)(using Context) =
    TeamPage(trans.team.newTeam.txt()).js(captchaEsm):
      main(cls := "page-menu page-small")(
        menu("form".some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(trt.newTeam()),
          postForm(cls := "form3", action := routes.Team.create)(
            form3.globalError(form),
            form3.group(form("name"), trans.site.name())(form3.input(_)),
            entryFields(form),
            textFields(form),
            renderCaptcha(form, captcha),
            form3.actions(
              a(href := routes.Team.home(1))(trans.site.cancel()),
              form3.submit(trt.newTeam())
            )
          )
        )
      )

  def edit(t: Team, form: Form[?], member: Option[TeamMember])(using ctx: Context) =
    TeamPage(s"Edit Team ${t.name}").js(Esm("bits.team")):
      main(cls := "page-menu page-small team-edit")(
        menu(none),
        div(cls := "page-menu__content box box-pad")(
          boxTop(h1("Edit team ", a(href := routes.Team.show(t.id))(t.name))),
          standardFlash,
          t.enabled.option(
            postForm(cls := "form3", action := routes.Team.update(t.id))(
              flairField(form),
              entryFields(form),
              textFields(form),
              accessFields(form),
              form3.actions(
                a(href := routes.Team.show(t.id))(trans.site.cancel()),
                form3.submit(trans.site.apply())
              )
            )
          ),
          hr,
          (t.enabled && (member.exists(_.hasPerm(_.Admin)) || Granter.opt(_.ManageTeam))).option(
            postForm(cls := "inline", action := routes.Team.disable(t.id))(
              explainInput,
              submitButton(
                dataIcon := Icon.CautionCircle,
                cls := "submit button text explain button-empty button-red",
                st.title := trans.team.closeTeamDescription.txt() // can actually be reverted
              )(trt.closeTeam())
            )
          ),
          Granter
            .opt(_.ManageTeam)
            .option(
              postForm(cls := "inline", action := routes.Team.close(t.id))(
                explainInput,
                submitButton(
                  dataIcon := Icon.Trash,
                  cls := "text button button-empty button-red explain",
                  st.title := "Deletes the team and its memberships. Cannot be reverted!"
                )(trans.site.delete())
              )
            ),
          (t.disabled && Granter.opt(_.ManageTeam)).option(
            postForm(cls := "inline", action := routes.Team.disable(t.id))(
              explainInput,
              submitButton(
                cls := "button button-empty explain",
                st.title := "Re-enables the team and restores memberships"
              )("Re-enable")
            )
          )
        )
      )

  private val explainInput = input(st.name := "explain", tpe := "hidden")

  private def flairField(form: Form[?])(using Context) =
    form3.flairPickerGroup(form("flair"), Flair.from(form("flair").value))

  private def textFields(form: Form[?])(using Context) = frag(
    form3.group(
      form("intro"),
      trans.team.introduction(),
      help = frag(trans.team.teamIntroductionHelp()).some
    )(
      form3.textarea(_)(rows := 2)
    )(cls := form("intro").value.isEmpty.option("accent")),
    form3.group(
      form("description"),
      trans.site.description(),
      help = frag(trans.team.teamDescriptionHelp(), br, markdownAvailable).some
    )(
      form3.textarea(_)(rows := 10)
    ),
    form3.group(
      form("descPrivate"),
      trans.site.descPrivate(),
      help = frag(
        trans.site.descPrivateHelp(),
        br,
        markdownAvailable
      ).some
    )(
      form3.textarea(_)(rows := 10)
    )
  )

  private def accessFields(form: Form[?])(using Context) =
    frag(
      form3.checkbox(
        form("hideMembers"),
        "Hide team member list from non-members.",
        half = true
      ),
      form3.split(
        form3.group(form("chat"), frag("Team chat"), help = frag("Who can use the team chat?").some) { f =>
          form3.select(
            f,
            Seq(
              Access.None.id -> "No chat",
              Access.Leaders.id -> "Team leaders",
              Access.Members.id -> "Team members"
            )
          )
        },
        form3.group(
          form("forum"),
          frag("Team forum"),
          help = frag(
            "Who can see the team forum on the team page?",
            br,
            "Only team members can post in the team forum."
          ).some
        ) { f =>
          form3.select(
            f,
            Seq(
              Access.Everyone.id -> "Show to everyone",
              Access.Members.id -> "Show to members",
              Access.Leaders.id -> "Show to team leaders",
              Access.None.id -> "Hide the forum"
            )
          )
        }
      )
    )

  private def entryFields(form: Form[?])(using ctx: Context) =
    form3.split(
      form3.checkbox(
        form("request"),
        trans.team.manuallyReviewAdmissionRequests(),
        help = trans.team.manuallyReviewAdmissionRequestsHelp().some,
        half = true
      ),
      form3.group(
        form("password"),
        trans.team.entryCode(),
        help = trans.team.entryCodeDescriptionForLeader().some,
        half = true
      )(form3.input(_))
    )
