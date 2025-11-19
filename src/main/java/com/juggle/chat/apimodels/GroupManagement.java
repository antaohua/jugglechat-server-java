package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GroupManagement {
    @JsonProperty("group_id")
    private String groupId;
    @JsonProperty("group_mute")
    private int groupMute;
    @JsonProperty("max_admin_count")
    private int maxAdminCount;
    @JsonProperty("admin_count")
    private int adminCount;
    @JsonProperty("group_verify_type")
    private int groupVerifyType;
    @JsonProperty("group_his_msg_visible")
    private int groupHisMsgVisible;

    @JsonProperty("group_edit_msg_right")
    private Integer groupEditMsgRight;

    @JsonProperty("group_add_member_right")
    private Integer groupAddMemberRight;

    @JsonProperty("group_mention_all_right")
    private Integer groupMentionAllRight;

    @JsonProperty("group_top_msg_right")
    private Integer groupTopMsgRight;

    @JsonProperty("group_send_msg_right")
    private Integer groupSendMsgRight;

    @JsonProperty("group_set_msg_life_right")
    private Integer groupSetMsgLifeRight;
}
