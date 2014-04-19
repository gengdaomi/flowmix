package org.calrissian.flowbot.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.calrissian.flowbot.FlowbotTopology;
import org.calrissian.flowbot.model.Event;
import org.calrissian.flowbot.model.FilterOp;
import org.calrissian.flowbot.model.Flow;
import org.calrissian.flowbot.model.PartitionOp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.calrissian.flowbot.Constants.*;
import static org.calrissian.flowbot.model.FilterOp.FILTER;
import static org.calrissian.flowbot.spout.MockFlowLoaderSpout.FLOW_LOADER_STREAM;

public class FilterBolt extends BaseRichBolt {

    Map<String,Flow> flows;
    OutputCollector collector;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        flows = new HashMap<String, Flow>();
    }

    @Override
    public void execute(Tuple tuple) {

        if(FLOW_LOADER_STREAM.equals(tuple.getSourceStreamId())) {
            for(Flow flow : (Collection<Flow>)tuple.getValue(0))
                flows.put(flow.getId(), flow);
        } else {
            String flowId = tuple.getStringByField(FLOW_ID);
            Event event = (Event) tuple.getValueByField(EVENT);
            int idx = tuple.getIntegerByField(FLOW_OP_IDX);
            idx++;

            Flow flow = flows.get(flowId);

            if(flow != null) {
                FilterOp filterOp = (FilterOp) flow.getFlowOps().get(idx);

                String nextStream = flow.getFlowOps().size() > idx+1 ? flow.getFlowOps().get(idx+1).getComponentName() : "output";
                if(filterOp.getCriteria().matches(event))
                    collector.emit(nextStream, tuple, new Values(flowId, event, idx));
            }

            collector.ack(tuple);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        FlowbotTopology.declareOutputStreams(outputFieldsDeclarer);
    }
}