package com.indeed.imhotep.commands;

import com.indeed.imhotep.CommandSerializationUtil;
import com.indeed.imhotep.GroupMultiRemapRule;
import com.indeed.imhotep.ImhotepRemoteSession;
import com.indeed.imhotep.api.ImhotepCommand;
import com.indeed.imhotep.api.ImhotepOutOfMemoryException;
import com.indeed.imhotep.api.ImhotepSession;
import com.indeed.imhotep.io.RequestTools.GroupMultiRemapRuleSender;
import com.indeed.imhotep.io.RequestTools.ImhotepRequestSender;
import com.indeed.imhotep.protobuf.ImhotepRequest;
import com.indeed.imhotep.protobuf.ImhotepResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections.IteratorUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@EqualsAndHashCode
@ToString
public class MultiRegroup implements ImhotepCommand<Integer> {

    @Getter(lazy =  true)  private final GroupMultiRemapRuleSender groupMultiRemapRuleSender = groupMultiRemapRuleSenderInitializer();
    private final GroupMultiRemapRule[] rules;
    private final boolean errorOnCollision;
    @Getter private final String sessionId;
    @Getter(lazy = true) private final ImhotepRequestSender imhotepRequestSender = imhotepRequestSenderInitializer();

    private MultiRegroup(final GroupMultiRemapRule[] rules, final boolean errorOnCollision, final String sessionId) {
        this.rules = rules;
        this.errorOnCollision = errorOnCollision;
        this.sessionId = sessionId;
    }

    public static MultiRegroup creatMultiRegroupCommand(final GroupMultiRemapRule[] rules, final boolean errorOnCollision, final String sessionId) {
        return new MultiRegroup(rules, errorOnCollision, sessionId);
    }

    public static MultiRegroup createMultiRegroupCommand(final int numRawRules, final Iterator<GroupMultiRemapRule> rawRules, final boolean errorOnCollision, final String sessionId) {
        return new MultiRegroup((GroupMultiRemapRule[])IteratorUtils.toArray(rawRules), errorOnCollision, sessionId);
    }

    private ImhotepRequestSender imhotepRequestSenderInitializer() {
        final ImhotepRequest header = ImhotepRequest.newBuilder()
                .setRequestType(ImhotepRequest.RequestType.EXPLODED_MULTISPLIT_REGROUP)
                .setSessionId(getSessionId())
                .setLength(getGroupMultiRemapRuleSender().getRulesCount())
                .setErrorOnCollisions(errorOnCollision)
                .build();

        return ImhotepRequestSender.Cached.create(header);
    }

    private  GroupMultiRemapRuleSender groupMultiRemapRuleSenderInitializer() {
        return GroupMultiRemapRuleSender.createFromRules(Arrays.asList(rules).iterator(), true);
    }

    @Override
    public Integer combine(final List<Integer> subResults) {
        return Collections.max(subResults);
    }

    @Override
    public void writeToOutputStream(final OutputStream os) throws IOException {
        ImhotepRemoteSession.sendRequestReadNoResponseFlush(getImhotepRequestSender(), os);
        getGroupMultiRemapRuleSender().writeToStreamNoFlush(os);
        os.flush();
    }

    public Integer apply(final ImhotepSession session) throws ImhotepOutOfMemoryException {
        return session.regroup(rules, errorOnCollision);
    }

    @Override
    public Integer readResponse(final InputStream is, final CommandSerializationUtil serializationUtil) throws IOException, ImhotepOutOfMemoryException {
        final ImhotepResponse imhotepResponse = ImhotepRemoteSession.readResponseWithMemoryExceptionSessionId(is, serializationUtil.getHost(), serializationUtil.getPort(), getSessionId());
        return imhotepResponse.getNumGroups();
    }

    @Override
    public Class<Integer> getResultClass() {
        return Integer.class;
    }
}
