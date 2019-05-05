#!/usr/bin/env ruby

require 'optparse'

RANGE=4

C_EPOCH=0
C_TIME=1
C_THREAD_NAME=2
C_TID=3

H_THREAD_NAME=0
H_TIME=1
H_TID=2
H_STACK=3

module Assign
  ASSIGNED = 0
  TOO_EARLY = 1
  OUT_OF_TS = 2
  MISSED_TS = 3
end

def assign(options,ctmap,topmap,hkey,hts,stack,htname)
  stamps = ctmap[hkey]
  top = topmap[hkey]

  if stamps[0][C_TIME].to_i > hts then
    # Acceptable since chappie starts the "application" after the program
    # has been running
    return Assign::TOO_EARLY
  end

  if top >= stamps.length then
    if options[:debug] and options[:verbose] then
      puts "Could not assign #{htname} with time #{hts}, out of entries"
    end
    return Assign::OUT_OF_TS
  end

  # Assign the first timestamp within the acceptable RANGE
  closest = nil
  closestind = -1
  for i in top...stamps.length
    if stamps[i][C_TIME].to_i-RANGE <= hts and stamps[i][C_TIME].to_i+RANGE >= hts then
      stamps[i][-1] = stack
      topmap[hkey] = i+1
      return Assign::ASSIGNED
    else
      distance = (stamps[i][C_TIME].to_i-hts).abs()
      if closest.nil? or distance < closest then
        closest = distance
        closestind = i
      end
    end
  end

  # We'll assign the closest timestamp IF its within an acceptable range to the stamp in question
  if closest <= RANGE * 3 then
    if options[:debug] and options[:verbose] then
      puts "Assigned #{htname} with time #{hts} with distance #{closest}. Caused a #{closestind-top} skip"
    end
    stamps[closestind][-1] = stack
    topmap[hkey] = closestind+1
    return Assign::ASSIGNED
  else
    if options[:debug] and options[:verbose] then
      puts "Could not assign #{htname} with time #{hts}"
    end
    return Assign::MISSED_TS
  end
end

def analyze(options)
  cf = File.open(options[:infile1])
  header = ""


  ctrace = [] # Raw trace
  ctmap = {}  # Represents a "stack" for each thread's timestamps
  topmap = {} # Represents the "top" of the thread timestamp "stack" we are using
  missedmap = {} # Track which threads do not get time stamps assigned
  idnamemap = {} # For debugging


  cf.each_line do |line|
    if line.include?("epoch")
      header = line.chop
      next
    end
    tok = line.split(",")
    tok[-1].chop!
    tok = tok[0,9]
    tok << "end"
    ctrace << tok

    # Build a mapping for faster timestamp alignment
    tname = tok[C_THREAD_NAME].split("#")[0]
    tid = tok[C_TID]
    tkey = if options[:usename] then tname else tid end
    unless ctmap.key?(tkey) then
      ctmap[tkey] = []
      topmap[tkey] = 0
      missedmap[tkey] = 0
      idnamemap[tkey] = tname
    end
    ctmap[tkey] << tok

  end

  header = header.split(",")
  header = header[0,9]
  header = header.join(",")
  header << ",stack"

  hf = File.open(options[:infile2])
  htrace = []
  hf.each_line do |line|
    tok = line.split(",")
    htrace << tok
  end

  missed = 0

  for j in 0...htrace.length
    hkey = nil
    if (options[:usename]) then
      hkey = htrace[j][H_THREAD_NAME]
    else
      hkey = htrace[j][H_TID]
    end
    hname =  htrace[j][H_THREAD_NAME]
    ts = htrace[j][H_TIME].to_i
    stack = htrace[j][H_STACK]

    if ctmap.key?(hkey) then
      r = assign(options,ctmap,topmap,hkey,ts,stack,hname)
      if (r == Assign::OUT_OF_TS or r == Assign::MISSED_TS) then
        missedmap[hkey] += 1
        missed += 1
      end
    else
      if options[:debug] then
        puts "thread #{hname} is not in map, missed timpestamp"
      end
      missed += 1
      next
    end
  end

  unless options[:debug] then
    puts header
    ctrace.each do |line|
      puts line.join(",")
    end
  else
    missedmap.each do |k,v|
      if v > 0 then
        if options[:usename] then
          puts "Thread #{k} missed #{v}"
        else
          name = idnamemap[k]
          puts "Thread #{name} missed #{v}"
        end
      end
    end
    puts "Number of stamps: #{htrace.length}"
    puts "Number missed: #{missed}"
  end
end

if __FILE__ == $0
  options = {}
  options[:infile] = ""
  options[:debug] = false
  options[:usename] = false
  options[:verbose] = false

  optparse = OptionParser.new do |opts|
    opts.on("-c", "=MANDATORY", "chappie thread csv") do |o|
      options[:infile1] = o.to_s
    end

    opts.on("-h", "=MANDATORY", "honest log") do |o|
      options[:infile2] = o.to_s
    end

    opts.on("-d", "turn on debugging") do |o|
      options[:debug] = true
    end

    opts.on("-v", "turn on verbose output") do |o|
      options[:verbose] = true
    end

    opts.on("-n", "use thread name instead of id for alignment") do |o|
      options[:usename] = true
    end
  end

  optparse.parse!

  analyze(options)

end
