# -*- coding: utf-8 -*-
import asyncio
import datetime
import os
import random
import re
import time
import certifi
from pyrogram import Client, filters
from pyrogram.errors import UsernameInvalid, UsernameNotOccupied, UsernameOccupied
# from pyrogram.raw.functions.account import CheckUsername
from pymongo import MongoClient

from pyrogram import raw,enums
from pprint import pprint
from pyrogram.errors import StatsMigrate

from pyrogram import types,utils
import pyrogram
import hashlib
import itertools
from concurrent.futures import ThreadPoolExecutor,as_completed
from opensearchpy import OpenSearch, RequestsHttpConnection
import requests

from langdetect import detect_langs, DetectorFactory
from langdetect.lang_detect_exception import LangDetectException

# 打印pryogram版本
print (pyrogram.__version__)

MONGO_HOST = 'mongodb+srv://*******************'


# 连接到mongodb
mongo_client = MongoClient(
    MONGO_HOST,
    tlsCAFile=certifi.where(),
    # SSL 配置
    ssl=True,
    # ssl_cert_reqs='CERT_NONE',
    # 连接池配置
    maxPoolSize=50,
    minPoolSize=10,
    maxIdleTimeMS=45000,
    # 超时配置
    connectTimeoutMS=5000,
    socketTimeoutMS=5000,
    serverSelectionTimeoutMS=5000
)
db = mongo_client['soso']  # Replace with your database name


# 连接到OpenSearch
host = '*******************.es.amazonaws.com'
port = 443
index_name = 'tg_soso'

es_client = OpenSearch(
    hosts = [f'{host}:{port}'],
    http_auth = ('soso', '*******************'),
    use_ssl = True,
    verify_certs = True,
    connection_class = RequestsHttpConnection
)

API_ID = 6
API_HASH = "eb06d4abfb49dc3eeb1aeb98ae0f581e"

# 从mongo中获取协议号
#accounts = db['sessions'].find({'status':1})
accounts = db['sessions'].find({'status':1})

# 打印协议号的数量
account_count = db['sessions'].count_documents({'status':1})
print (f'共有{account_count}个协议号')

# 打乱排序
accounts = list(accounts)
# 打乱排序
import random
random.shuffle(accounts)
accounts_files_cycler = itertools.cycle(accounts)

# 获取代理
def get_proxy():
    user = '*******************'
    token = "*******************"

    url = 'https://dvapi.doveproxy.net/cmapi.php'

    # 随机代理地区
    # geos = ['tr','za','br','id','us','ru','th']
    geos = ['in'] # 代理地区 https://www.doveip.com/index.php?s=/documentation/country.html&lang=zh-cn
    params = {
        'rq': 'distribute',
        'user': user,
        'token': token,
        'auth' : 1,
        'geo': random.choice(geos), # 代理地区 https://www.doveip.com/index.php?s=/documentation/country.html&lang=zh-cn
        'num': 1,
        'type_ip': 2,#协议类型(可选，不传为获取所有,1为获取IPv4 2为获取IPv6)
        'agreement': 0, #代理协议(默认配置为0,0为Socks5协议代理，1为HTTP协议代理)
    }
    response = requests.get(url, params=params)
    #案例: {'errno': 200, 'msg': 'Success', 'data': {'geo': 'id', 'ip': '18.139.39.145', 'port': 40000, 'd_ip': '182.2.40.81', 'timeout': 600, 'username': 'agoukuaile', 'password': '136477060eed49f'}}
    
    if response.status_code != 200:
        print('⚠️ 获取代理失败')
        return None
    
    #如果返回的数据中没有errno字段或者errno不等于200，则代理获取失败
    if 'errno' not in response.json() or response.json()['errno'] != 200:
        print('⚠️ 获取代理失败',response.json())
        return None

    
    print('✅ 获取代理成功',response.json())
    return response.json()['data']


# 查询条件
query = {
    '$and': [
        {'$or': [
            {'status': 'OK'},
        ]},
        {'$or': [
            {'chat_created_at': {'$exists': False}},
            {'spider_at': {'$exists': False}},
            {'spider_at': {'$lt': datetime.datetime.now() - datetime.timedelta(days=1)}},
        ]}
    ]
}



all_chats_info = db['chat'].find(query).sort('_id', -1).limit(2000)
chats_info_count = db['chat'].count_documents(query)


print (f'共有{chats_info_count}个Chat需要采集')

# 延迟3秒
time.sleep(3)

# 检测到儿童色情搜索，封禁频道
def ban_jisou_group(chat_info,user):
    # 儿童色情机器人消息，写入到mongo的日志
    data = {
        'chat_id': chat_info['chat_id'],
        'type': 'BANCHAT',
        'info' : f'因为检测到诈骗搜索机器人JISOU相关机器人在群组中发的是管理员而封禁频道。恶意机器人用户名 @{user.username}',
        'created_at': datetime.datetime.now()
    }
    db['chat_log'].insert_one(data)

    # 写入到封禁频道任务
    data = {
        'chat_id':chat_info['chat_id'],
        'chat': chat_info["link"],
        'type': 'BANCHAT',
        'info' : f'因为检测到诈骗搜索机器人JISOU相关机器人在群组中发的是管理员而封禁频道。恶意机器人用户名 @{user.username}',
        'created_at': datetime.datetime.now(),
        'status': 0 # 0未处理，1已处理
    }

    db['ban_chat'].update_one({'chat_id':chat_info['chat_id']},{'$set':data},upsert=True)

    # 从 mongo中把 chat 的 status 改为 'BAN'
    db['chat'].update_one({'chat_id': chat_info['chat_id']}, {'$set': {'status': 'BAN'}})


# 从es中删除信息
def es_delete(id):
    try:
        result = es_client.delete(index='tg_soso', id=id)
        if result['result'] == 'deleted':
            return {'status': 'success', 'text': '已删除'}
        else:
            return {'status': 'error', 'text': '删除失败,请联系管理员\n错误信息：' + str(result)}
    except Exception as e:
        return {'status': 'error', 'text': '删除失败,请联系管理员\n错误信息：' + str(e)}

def contains_target_languages(text, target_languages):
    try:
        detected_languages = detect_langs(text)
        # print(text,detected_languages)
        for language in detected_languages:
            if language.lang in target_languages:
                return True
    except LangDetectException:
        return False
    return False

# 儿童色情搜索检测工具
def check_jisou(message:types.Message):
    # print (message.id)
    jisous = ['@jisou ','/jisou?','/jisou123','/jisou2bot','/jisou1bot']
    # 检测超级链接
    if message.entities:
        for entity in message.entities:
            if entity.type == 'url':
                if any(jisou in entity.url.lower() for jisou in jisous):
                    print (f'消息中url', entity.url)
                    return True
                
    if message.text:
        if any(jisou in message.text.lower() for jisou in jisous):
            print (f'消息中文本', message.text)
            return True
    
    if message.caption:
        if any(jisou in message.caption.lower() for jisou in jisous):
            print (f'消息中caption', message.caption)
            return True
        
    if message.reply_markup:
        for buttons in message.reply_markup.inline_keyboard:
            for button in buttons:
                if button.url:
                    if any(jisou in button.url.lower() for jisou in jisous):
                        print (f'消息中链接按钮', button.url)
                        return True
                if button.callback_data and '/start/invitation' in button.callback_data:
                    print (f'消息中按钮', button.callback_data)
                    return True
    return False

# 检测到儿童色情搜索，封禁频道
def ban_jisou(message:types.Message):
    # 儿童色情机器人消息，写入到mongo的日志
    data = {
        'chat_id': message.chat.id,
        'type': 'BANCHAT',
        'info' : f'因为检测到诈骗搜索机器人JISOU而封禁频道。 消息链接： https://t.me/{message.chat.username}/{message.id}',
        'created_at': datetime.datetime.now()
    }
    db['chat_log'].insert_one(data)

    # 写入到封禁频道任务
    data = {
        'chat_id': message.chat.id,
        'chat': message.chat.username,
        'type': 'BANCHAT',
        'info' : f'因为检测到诈骗搜索机器人JISOU而封禁频道。 消息链接： https://t.me/{message.chat.username}/{message.id}',
        'created_at': datetime.datetime.now(),
        'status': 0 # 0未处理，1已处理
    }
    db['ban_chat'].update_one({'chat_id':message.chat.id},{'$set':data},upsert=True)

    # 写入到异步通知
    data = {
        'chat_id': message.chat.id,
        'type': 'BANCHAT',
        'info' : f'因为检测到诈骗搜索机器人JISOU而封禁频道。 消息链接： https://t.me/{message.chat.username}/{message.id}',
        'created_at': datetime.datetime.now(),
        'notify': 0 # 0未处理，1已处理
    }
    db['notify'].insert_one(data)



# 读取Chat的信息
async def get_channel_msg(chats_info,name = '未命名线程'):
    print (f'{name}开始采集',len(chats_info))
    # print (f'采集范围：{start} - {end}')
    # 打印chats_info
    # proxy_info = get_proxy()
    # if proxy_info is None:
    #     print('⚠️ 代理获取失败')
    #     return
    for chat_info in chats_info:
        if 'link' not in chat_info:
            print ('没有链接，跳过',chat_info)
            continue
        print (name,'开始登录')
        account = next(accounts_files_cycler)
        app = Client(
            name=str(account['tg_id']),
            api_id=API_ID,
            api_hash=API_HASH,
            session_string=account['pyrogram'],
            takeout=True,
            lang_code='zh-hans',
            # connection_retries=5,
            # retry_delay=3
            # proxy={
            #     'scheme': 'socks5',
            #     'hostname': proxy_info['ip'],
            #     'port': proxy_info['port'],
            #     'username': proxy_info['username'],
            #     'password': proxy_info['password'],
            # }
        )
        try:
            await app.start()
            print (name,'登录成功')
        except Exception as e:
            print (name,'登录失败',e)
            del_account = ['deleted','AUTH_KEY_UNREGISTERED','AUTH_KEY_DUPLICATED','USER_DEACTIVATED_BAN','SESSION_REVOKED']
            if any(i in str(e) for i in del_account):
                print ('账号已注销',account['tg_id'])
                # 把账号设置status为0
                db['sessions'].update_one({'tg_id':account['tg_id']},{'$set':{'status':0}})
                print (account['tg_id'],'已删除🚮')
            continue
        # 获取个人信息
        # me = app.get_me()
        # print (me)
        # 取消敏感内容限制
        # result = await app.invoke(
        #     query=raw.functions.account.SetContentSettings(
        #         sensitive_enabled=True
        #     )
        # )
        try:
            await app.invoke(
                raw.functions.account.SetContentSettings(
                    sensitive_enabled=True
                )
            )
            print ('取消敏感内容限制成功')
        except Exception as e:
            print ('取消敏感内容限制失败',e)
            if 'FROZEN_METHOD_INVALID' in str(e):
                print ('账号无法取消限制',account['tg_id'])
                # 把账号设置status为0
                db['sessions'].update_one({'tg_id':account['tg_id']},{'$set':{'status':0}})
                print (account['tg_id'],'已删除🚮')
                
            continue #这里要跳出，因为如果取消失败则意味着可能无法正常获取频道信息
        
        print (account['tg_id'],'已登录',name)
        username = chat_info['link'].replace('https://t.me/','').lower()
        try:
            # 延迟2秒 (我都换号了，我延迟什么)
            # time.sleep(2)
            # 获取频道的信息
            print (f'{name}开始获取频道信息',username)
            try:
                get_chat:types.Chat = await app.get_chat(username)
            except Exception as e:
                print (e, username,'获取频道信息失败')
                not_found = ['Username not found','username is invalid','username is not occupied']
                if any(i in str(e) for i in not_found):
                    # 从 mongo中把 chat 的 status 改为 'DELETED'
                    db['chat'].update_one({'chat_id': chat_info['chat_id']}, {'$set': {'status': 'DELETED'}})
                    # db['chat'].delete_one({'chat_id': chat_info['chat_id']})
                    # 临时改为直接删除

                    # 写入到chatLog
                    data = {
                        'chat_id': chat_info['chat_id'],
                        'type': 'CHATDEL',
                        'info' : f'CHAT用户名不存在而自动封禁。用户名 @{username}',
                        'created_at': datetime.datetime.now()
                    }
                    db['chat_log'].insert_one(data)
                    
                    # 尝试 从 Es中删除该聊天(群组或频道)信息
                    try:
                        es_delete(chat_info['chat_id'])
                    except Exception as e:
                        print('删除Es中该聊天(群组或频道)信息失败:',e)
                    print('⚠️⚠️⚠️CHAT不存在，删除频道信息',username)
                continue
            print (f'{name}获取频道信息成功',username)
            if get_chat.type.name not in ['CHANNEL','SUPERGROUP']:
                db['chat'].update_one({'chat_id': chat_info['chat_id']}, {'$set': {'status': 'DELETED'}})
                data = {
                    'chat_id': chat_info['chat_id'],
                    'type': 'CHATDEL',
                    'info' : f'CHAT类型错误，不是频道或超级群组而自动封禁。用户名 @{username} 类型 {get_chat.type.name}',
                    'created_at': datetime.datetime.now()
                }
                # 尝试 从 Es中删除该聊天(群组或频道)信息
                try:
                    es_delete(chat_info['chat_id'])
                except Exception as e:
                    print('删除Es中该聊天(群组或频道)信息失败:',e)
                print('⚠️⚠️⚠️CHAT类型错误',get_chat.type.name)
                continue
            # 初始化基本信息
            usernames = []
            if get_chat.usernames is not None and len(get_chat.usernames) > 0:
                for nft_username in get_chat.usernames:
                    usernames.append(nft_username.username)
            data = {
                'title': get_chat.title,
                'username': get_chat.username,
                'type': get_chat.type.name,
                'link': f'https://t.me/{get_chat.username}',
                'members_count': get_chat.members_count,
                'is_verified': get_chat.is_verified,
                'usernames': usernames,
                'updated_at': datetime.datetime.now(),
                'spider_at': datetime.datetime.now(),
                #'status':'OK'
            }
            # 判断是否色情限制
            if get_chat.restrictions:
                data['porn'] = True

            # 获取 chat 的类型
            chat_type = get_chat.type.name

            try:
                # 首先，我们需要获取 channel 的 InputPeer
                peer = await app.resolve_peer(username)
                
                # 确保 peer 是 InputPeerChannel 类型
                if isinstance(peer, raw.types.InputPeerChannel):
                    # 创建 InputChannel 对象
                    input_channel = raw.types.InputChannel(
                        channel_id=peer.channel_id,
                        access_hash=peer.access_hash
                    )
                    result = await app.invoke(
                        raw.functions.channels.GetFullChannel(
                            channel=input_channel
                        )
                    )
                    # data['chat_created_at'] = datetime.datetime.fromtimestamp(result.chats[0].date) # 这个不准
                    if chat_type == 'SUPERGROUP':
                        data['online_count'] = result.full_chat.online_count

                    # 获取 chat 的创建时间
                    first_message = await app.get_messages(username, message_ids=1)
                    if first_message:
                        data['chat_created_at'] = first_message.date
                        print ('获取频道创建时间成功',first_message.date)
                    else:
                        print('获取频道创建时间失败')

                else:
                    print("The provided ID is not a channel")
            except Exception as e:
                print(f"发生错误 GetFullChannel: {e}")



            if chat_type == 'SUPERGROUP':
                print ('超级群组，检查是否有垃圾机器人')
                # admins = await app.get_chat_members(username,filter=enums.ChatMembersFilter.ADMINISTRATORS)
                # 需要封杀的机器人
                botIDs = [5770101555,6213379764,5762373625,6615731424,6265676785]
                admins = []
                async for admin in app.get_chat_members(username,filter=enums.ChatMembersFilter.ADMINISTRATORS):
                    if admin.user.is_bot:
                        print ('🤖发现机器人',admin.user.username,admin.user.first_name,username)
                        if  admin.user.id in botIDs:
                            # 群组中发现恶意病毒机器人
                            print("⚠️⚠️⚠️儿童色情搜索机器人",admin.user.username)
                            ban_jisou_group(chat_info,admin.user)
                            break
                    admins.append(admin.user.id)
            
                data['admins'] = admins

            elif chat_type == 'CHANNEL':

                # 判断是否有 linked_chat 
                if get_chat.linked_chat:
                    print ('频道有 linked_chat',get_chat.linked_chat.id)

                    data['linked_chat'] = {
                        'chat_id': get_chat.linked_chat.id,
                        'title': get_chat.linked_chat.title,
                        'username': get_chat.linked_chat.username,
                    }

                print ('频道，准备获取频道消息数量',username)
                # 获取ccav频道的最新一条消息
                async for message in app.get_chat_history(username, limit=1):
                    data['last_message_date'] = message.date
                # 获取频道的的完整信息
                try:
                    # 首先，我们需要获取 channel 的 InputPeer # 上面获取过了
                    # peer = await app.resolve_peer(username)  
                    # 创建 GetSearchCountersRequest
                    request = raw.functions.messages.GetSearchCounters(
                        peer=peer,
                        filters=[
                            raw.types.InputMessagesFilterPhotos(),
                            raw.types.InputMessagesFilterVideo(),
                            raw.types.InputMessagesFilterVoice(),
                            raw.types.InputMessagesFilterMusic(),
                        ]
                    )
                    # 发送请求
                    counters = await app.invoke(request)
                    for counter in counters:
                        # print(f"Type: {counter.filter.__class__.__name__}, Count: {counter.count}")
                        if counter.filter.__class__.__name__ == 'InputMessagesFilterPhotos':
                            data['photo_count'] = counter.count
                        elif counter.filter.__class__.__name__ == 'InputMessagesFilterVideo':
                            data['video_count'] = counter.count
                        elif counter.filter.__class__.__name__ == 'InputMessagesFilterVoice':
                            data['voice_count'] = counter.count
                        elif counter.filter.__class__.__name__ == 'InputMessagesFilterMusic':
                            data['music_count'] = counter.count
                    print ('成功获取频道消息数量',data)
                except Exception as e:
                    print(f"获取分类消息数量错误: {e}")
                msg_count = await app.get_chat_history_count(username)
                print ('频道总消息数量',msg_count)
                data['msg_count'] = msg_count

                # 如果chat没有recommend字段，则调用接口获取
                # if 'recommend' not in chat_info :
                    # print ('频道没有recommend字段，调用接口获取')
                if 1 == 1: # 管他有没有，就要获取，以保证自动新增收录
                    recommends = await app.invoke(raw.functions.channels.GetChannelRecommendations(channel=peer))
                    recommend_list = []
                    for recommend in recommends.chats:
                        # 如果没有username，跳过
                        if not recommend.username and not recommend.usernames:
                            continue
                        if not recommend.username:
                            channel_username = recommend.usernames[0].username
                        else:
                            channel_username = recommend.username

                        # 检查标题是否包含俄语（ru）或波斯语（fa）
                        # target_languages = ['ru', 'fa','bg','vi']
                        # contains_russian_or_persian = contains_target_languages(recommend.title,target_languages)
                        # if contains_russian_or_persian:
                        #     print ('⚠️⚠️⚠️检测到俄语或波斯语',recommend.title)
                        #     continue

                        # 判断标题是否有中文
                        if not bool(re.search('[\u4e00-\u9fff]', recommend.title)):
                            print ('⚠️⚠️⚠️未检测到中文',recommend.title)
                            continue

                        recommend_list.append({
                            'title': recommend.title,
                            'username': channel_username,
                            'chat_id': utils.get_channel_id(recommend.id),
                            'members_count': recommend.participants_count
                        })
                        # 查询chat是否存在
                        recommend_chat = db['chat'].find_one({'chat_id': utils.get_channel_id(recommend.id)})
                        if recommend_chat is None:
                            print ('✅获取新的到相似频道',recommend.title)
                            # 写入到mongo
                            data = {
                                'chat_id': utils.get_channel_id(recommend.id),
                                'title': recommend.title,
                                'username': channel_username,
                                'type': 'CHANNEL',
                                'link': f'https://t.me/{channel_username}',
                                'members_count': recommend.participants_count,
                                'from_chat': chat_info['chat_id'],
                                'status': 'OK',
                                'created_at': datetime.datetime.now()
                            }
                            if recommend.usernames:
                                data['usernames'] = [username.username for username in recommend.usernames]
                                print ('✅获取到相似频道NFT用户名',data['usernames'])
                            db['chat'].insert_one(data)
                    data['recommend'] = recommend_list


            print (f'{name}开始更新频道信息',username)
            # 更新到mongo
            db['chat'].update_one({'chat_id': chat_info['chat_id']}, {'$set': data})
            print (f'✅{name}更新频道信息成功',username,data['type'])
            # 这时候我们再判断是否有儿童色情搜索
            if get_chat.pinned_message and check_jisou(get_chat.pinned_message):
                print ('⚠️⚠️⚠️检测到儿童色情搜索')
                try:
                    ban_jisou(get_chat.pinned_message)
                except Exception as e:
                    print ('封禁频道失败',e)

            await app.stop()
        except Exception as e:
            print ('循环过程中报错',e)
            # 打印错误行数
            import traceback
            print (traceback.format_exc())
            continue
    # try:
    #     await app.stop()
    # except Exception as e:
    #     print ('结束时报错',e)
    print (f'{name}采集结束')



from concurrent.futures import ThreadPoolExecutor

async def spider(chats_info, name):
    print('全部chat', len(chats_info))
    print(name, '开始爬取')
    await get_channel_msg(chats_info, name)
    print(name, '爬取结束')

async def main():
    global all_chats_info
    all_chats_info = list(all_chats_info) 
    chats_info_count = len(all_chats_info)

    # 一个任务处理的chat数量
    task_count = 20
    tasks = []

    for i in range(0, chats_info_count, task_count):
        chats_info = all_chats_info[i:min(i + task_count, chats_info_count)]
        task = asyncio.create_task(spider(chats_info, f'任务{i // task_count + 1}'))
        tasks.append(task)

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())


